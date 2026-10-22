package com.shohankhan.nexcalc.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Local-only Room database. No destructive migrations are configured: schema changes must be
 * shipped as explicit migrations, and version 1 is the initial schema.
 */
@Database(
    entities = [HistoryRecord::class, SavedRecord::class, FavoriteRecord::class],
    version = 1,
    exportSchema = false
)
abstract class NexCalcDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
    abstract fun savedDao(): SavedDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        private const val NAME = "nexcalc.db"

        @Volatile
        private var instance: NexCalcDatabase? = null

        fun get(context: Context): NexCalcDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, NexCalcDatabase::class.java, NAME)
                    .build()
                    .also { instance = it }
            }
    }
}
