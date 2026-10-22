package com.shohankhan.nexcalc.core.data

import com.shohankhan.nexcalc.core.data.local.HistoryDao
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Owns calculation history. Records are written only when the user explicitly evaluates, so
 * recomposition and configuration changes can never duplicate rows; a short same-expression window
 * also absorbs accidental double taps.
 */
class HistoryRepository(
    private val dao: HistoryDao,
    private val preferences: PreferencesRepository
) {

    fun observeAll(): Flow<List<HistoryRecord>> = dao.observeAll()

    fun observeFiltered(query: String, source: String): Flow<List<HistoryRecord>> =
        dao.observeFiltered(query.trim(), source)

    suspend fun findRecentDuplicate(expression: String, source: String): HistoryRecord? =
        dao.findRecentDuplicate(expression, source, System.currentTimeMillis() - DUPLICATE_WINDOW_MS)

    suspend fun record(expression: String, result: String, source: String, detail: String? = null): Boolean {
        val trimmedExpression = expression.trim()
        if (trimmedExpression.isEmpty()) return false
        if (dao.findRecentDuplicate(trimmedExpression, source, System.currentTimeMillis() - DUPLICATE_WINDOW_MS) != null) {
            return false
        }
        dao.insert(
            HistoryRecord(
                expression = trimmedExpression,
                result = result,
                detail = detail,
                source = source,
                createdAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun delete(ids: List<Long>) {
        if (ids.isNotEmpty()) dao.deleteByIds(ids)
    }

    /** Re-inserts records for the History undo action. New ids are assigned by Room. */
    suspend fun restore(records: List<HistoryRecord>) {
        if (records.isEmpty()) return
        dao.insertAll(records.map { it.copy(id = 0) })
    }

    suspend fun clear() = dao.deleteAll()

    suspend fun exportAll(): List<HistoryRecord> = dao.latestForExport()

    suspend fun importAll(records: List<HistoryRecord>) {
        if (records.isEmpty()) return
        dao.insertAll(records.map { it.copy(id = 0) })
    }

    suspend fun count(): Int = dao.count()

    /**
     * Removes records older than the retention window. Idempotent: repeated calls delete nothing
     * once the database matches the window. Returns the number of removed rows.
     */
    suspend fun cleanupNow(): Int {
        val retentionDays = preferences.settings.first().historyRetentionDays
        if (retentionDays < 0) return 0
        val cutoff = System.currentTimeMillis() - retentionDays * 24L * 60L * 60L * 1000L
        return dao.deleteOlderThan(cutoff)
    }

    companion object {
        const val DUPLICATE_WINDOW_MS = 1_500L
    }
}
