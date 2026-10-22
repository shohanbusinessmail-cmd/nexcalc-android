package com.shohankhan.nexcalc.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 2000")
    fun observeAll(): Flow<List<HistoryRecord>>

    @Query(
        "SELECT * FROM history WHERE (:query = '' OR expression LIKE '%' || :query || '%' OR result LIKE '%' || :query || '%') " +
            "AND (:source = 'ALL' OR source = :source) ORDER BY createdAt DESC LIMIT 2000"
    )
    fun observeFiltered(query: String, source: String): Flow<List<HistoryRecord>>

    @Query("SELECT * FROM history WHERE expression = :expression AND source = :source AND createdAt >= :since ORDER BY createdAt DESC LIMIT 1")
    suspend fun findRecentDuplicate(expression: String, source: String, since: Long): HistoryRecord?

    @Query("SELECT * FROM history ORDER BY createdAt DESC LIMIT 500")
    suspend fun latestForExport(): List<HistoryRecord>

    @Insert
    suspend fun insert(record: HistoryRecord): Long

    @Insert
    suspend fun insertAll(records: List<HistoryRecord>)

    @Query("DELETE FROM history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM history WHERE createdAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM history")
    suspend fun count(): Int
}

@Dao
interface SavedDao {

    @Query("SELECT * FROM saved ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedRecord>>

    @Query("SELECT * FROM saved WHERE kind = :kind ORDER BY createdAt DESC")
    fun observeByKind(kind: String): Flow<List<SavedRecord>>

    @Query("SELECT * FROM saved WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): SavedRecord?

    @Insert
    suspend fun insert(record: SavedRecord): Long

    @Insert
    suspend fun insertAll(records: List<SavedRecord>)

    @Update
    suspend fun update(record: SavedRecord)

    @Query("DELETE FROM saved WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM saved")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM saved")
    suspend fun count(): Int
}

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FavoriteRecord>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE `key` = :key)")
    fun observeIsFavorite(key: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: FavoriteRecord)

    @Query("DELETE FROM favorites WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM favorites")
    suspend fun all(): List<FavoriteRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<FavoriteRecord>)

    @Query("DELETE FROM favorites")
    suspend fun deleteAll()
}
