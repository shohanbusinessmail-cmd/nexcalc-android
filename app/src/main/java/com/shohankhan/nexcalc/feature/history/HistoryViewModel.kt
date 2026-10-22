package com.shohankhan.nexcalc.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.di.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId

data class HistoryDayGroup(val epochDay: Long, val records: List<HistoryRecord>)

data class HistoryUiState(
    val query: String = "",
    val source: String = "ALL",
    val sources: List<String> = emptyList(),
    val groups: List<HistoryDayGroup> = emptyList(),
    val selectionMode: Boolean = false,
    val selected: Set<Long> = emptySet(),
    val total: Int = 0
) {
    val selectedCount: Int get() = selected.size
}

/**
 * History state: query + source filter, day grouping, multi-select and undo support.
 *
 * Deletions return the removed rows so the UI can offer a real undo instead of an irreversible
 * confirmation.
 */
class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    private val query = MutableStateFlow("")
    private val source = MutableStateFlow("ALL")
    private val selectionMode = MutableStateFlow(false)
    private val selected = MutableStateFlow<Set<Long>>(emptySet())

    private val allRecords = container.historyRepository.observeAll()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filtered = combine(query, source) { q, s -> q to s }
        .flatMapLatest { (q, s) -> container.historyRepository.observeFiltered(q, s) }

    val state: StateFlow<HistoryUiState> = combine(
        filtered,
        allRecords,
        selectionMode,
        selected
    ) { records, all, selecting, selection ->
        HistoryUiState(
            query = query.value,
            source = source.value,
            sources = all.map { it.source }.distinct().sorted(),
            groups = group(records),
            selectionMode = selecting,
            selected = selection,
            total = all.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setSource(value: String) {
        source.value = value
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode.value = enabled
        if (!enabled) selected.value = emptySet()
    }

    fun toggleSelection(id: Long) {
        selected.value = if (id in selected.value) selected.value - id else selected.value + id
    }

    suspend fun deleteSelected(): List<HistoryRecord> {
        val ids = selected.value
        if (ids.isEmpty()) return emptyList()
        val removed = state.value.groups.flatMap { it.records }.filter { it.id in ids }
        container.historyRepository.delete(ids.toList())
        selected.value = emptySet()
        selectionMode.value = false
        return removed
    }

    suspend fun deleteOne(record: HistoryRecord): List<HistoryRecord> {
        container.historyRepository.delete(listOf(record.id))
        return listOf(record)
    }

    suspend fun clearAll(): List<HistoryRecord> {
        val removed = state.value.groups.flatMap { it.records }
        container.historyRepository.clear()
        selected.value = emptySet()
        selectionMode.value = false
        return removed
    }

    suspend fun restore(records: List<HistoryRecord>) {
        container.historyRepository.restore(records)
    }

    companion object {
        fun group(records: List<HistoryRecord>): List<HistoryDayGroup> {
            val zone = ZoneId.systemDefault()
            return records
                .groupBy { Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate().toEpochDay() }
                .toSortedMap(compareByDescending { it })
                .map { (day, items) -> HistoryDayGroup(day, items) }
        }
    }
}
