package com.shohankhan.nexcalc.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.search.SearchGroup
import com.shohankhan.nexcalc.core.search.SearchHit
import com.shohankhan.nexcalc.core.search.SearchTarget
import com.shohankhan.nexcalc.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SearchUiState(val query: String = "", val hits: List<SearchHit> = emptyList())

/**
 * Global offline search: static catalogue first, then the user's own history, saved work and
 * favorites. Results are ranked and capped so the list stays responsive while typing.
 */
class SearchViewModel(private val container: AppContainer) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            query.collect { value ->
                _state.value = SearchUiState(value, buildResults(value))
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    private suspend fun buildResults(value: String): List<SearchHit> {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return emptyList()
        val static = container.searchIndex.search(trimmed, limit = 40)
        val history = container.historyRepository.observeFiltered(trimmed, "ALL").first()
            .take(10)
            .map { record ->
                SearchHit(
                    key = "history:${record.id}",
                    title = record.expression,
                    subtitle = "= ${record.result}",
                    group = SearchGroup.HISTORY,
                    target = SearchTarget.Screen("history"),
                    score = 500
                )
            }
        val saved = container.savedRepository.observeAll().first()
            .filter { it.title.contains(trimmed, ignoreCase = true) || it.payload.contains(trimmed, ignoreCase = true) }
            .take(10)
            .map { record ->
                SearchHit(
                    key = "saved:${record.id}",
                    title = record.title,
                    subtitle = record.savedKind.name.lowercase().replace('_', ' '),
                    group = SearchGroup.SAVED,
                    target = SearchTarget.Screen("saved"),
                    score = 450
                )
            }
        val favorites = container.savedRepository.allFavorites()
            .filter { it.label.contains(trimmed, ignoreCase = true) }
            .take(10)
            .map { favorite ->
                SearchHit(
                    key = "favorite:${favorite.key}",
                    title = favorite.label,
                    subtitle = favorite.subtitle,
                    group = SearchGroup.FAVORITE,
                    target = targetFor(favorite.key),
                    score = 480
                )
            }
        return (static + history + saved + favorites)
            .sortedWith(compareByDescending<SearchHit> { it.score }.thenBy { it.title })
            .take(80)
    }

    private fun targetFor(key: String): SearchTarget {
        val kind = key.substringBefore(':')
        val id = key.substringAfter(':', "")
        return when (kind) {
            "FORMULA" -> SearchTarget.Formula(id)
            "TOOL" -> SearchTarget.QuickTool(id)
            "UNIT" -> SearchTarget.UnitCategory(id)
            else -> SearchTarget.Screen("saved")
        }
    }
}
