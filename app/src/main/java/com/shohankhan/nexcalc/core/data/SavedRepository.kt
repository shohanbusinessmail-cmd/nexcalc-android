package com.shohankhan.nexcalc.core.data

import com.shohankhan.nexcalc.core.data.local.FavoriteDao
import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.core.data.local.SavedDao
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.core.data.local.SavedRecord
import kotlinx.coroutines.flow.Flow

/** Personal workspace and favorites. */
class SavedRepository(
    private val savedDao: SavedDao,
    private val favoriteDao: FavoriteDao
) {

    fun observeAll(): Flow<List<SavedRecord>> = savedDao.observeAll()

    fun observeKind(kind: SavedKind): Flow<List<SavedRecord>> = savedDao.observeByKind(kind.name)

    fun observeFavorites(): Flow<List<FavoriteRecord>> = favoriteDao.observeAll()

    fun observeIsFavorite(key: String): Flow<Boolean> = favoriteDao.observeIsFavorite(key)

    suspend fun save(kind: SavedKind, title: String, payload: String, note: String = ""): Long =
        savedDao.insert(
            SavedRecord(
                kind = kind.name,
                title = title,
                payload = payload,
                note = note,
                createdAt = System.currentTimeMillis()
            )
        )

    suspend fun find(id: Long): SavedRecord? = savedDao.findById(id)

    suspend fun updateNote(id: Long, note: String) {
        val record = savedDao.findById(id) ?: return
        savedDao.update(record.copy(note = note))
    }

    suspend fun delete(ids: List<Long>) {
        if (ids.isNotEmpty()) savedDao.deleteByIds(ids)
    }

    suspend fun clearSaved() = savedDao.deleteAll()

    suspend fun toggleFavorite(kind: String, id: String, label: String, subtitle: String, favorite: Boolean) {
        val key = favoriteKey(kind, id)
        if (favorite) {
            favoriteDao.upsert(FavoriteRecord(key, kind, label, subtitle, System.currentTimeMillis()))
        } else {
            favoriteDao.delete(key)
        }
    }

    suspend fun removeFavorite(key: String) = favoriteDao.delete(key)

    suspend fun allFavorites(): List<FavoriteRecord> = favoriteDao.all()

    suspend fun importFavorites(records: List<FavoriteRecord>) {
        if (records.isNotEmpty()) favoriteDao.insertAll(records)
    }

    suspend fun importSaved(records: List<SavedRecord>) {
        if (records.isNotEmpty()) savedDao.insertAll(records.map { it.copy(id = 0) })
    }

    suspend fun clearFavorites() = favoriteDao.deleteAll()

    suspend fun counts(): Pair<Int, Int> = savedDao.count() to favoriteDao.all().size

    companion object {
        const val KIND_FORMULA = "FORMULA"
        const val KIND_TOOL = "TOOL"
        const val KIND_UNIT = "UNIT"

        fun favoriteKey(kind: String, id: String): String = "$kind:$id"
    }
}
