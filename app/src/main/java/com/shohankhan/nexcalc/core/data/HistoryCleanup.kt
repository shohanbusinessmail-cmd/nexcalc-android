package com.shohankhan.nexcalc.core.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shohankhan.nexcalc.core.data.local.NexCalcDatabase
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Retention is enforced in two places: once at app start and once a day through WorkManager.
 * Both paths call the same idempotent delete query, so overlapping execution cannot corrupt data.
 */
class HistoryCleanupWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val preferences = PreferencesRepository(applicationContext)
        val retentionDays = try {
            preferences.settings.first().historyRetentionDays
        } catch (error: Exception) {
            return Result.retry()
        }
        if (retentionDays < 0) return Result.success()
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        return try {
            NexCalcDatabase.get(applicationContext).historyDao().deleteOlderThan(cutoff)
            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }
}

object HistoryCleanupScheduler {

    private const val UNIQUE_WORK = "nexcalc-history-cleanup"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
