package com.shohankhan.nexcalc.feature.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.core.data.local.SavedRecord
import com.shohankhan.nexcalc.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val items: List<SavedRecord> = emptyList(),
    val favorites: List<FavoriteRecord> = emptyList(),
    val filter: SavedKind? = null
)

class SavedWorkspaceViewModel(private val container: AppContainer) : ViewModel() {

    private val filter = MutableStateFlow<SavedKind?>(null)

    val state: StateFlow<SavedUiState> = combine(
        container.savedRepository.observeAll(),
        container.savedRepository.observeFavorites(),
        filter
    ) { items, favorites, selected ->
        SavedUiState(
            items = if (selected == null) items else items.filter { it.savedKind == selected },
            favorites = favorites,
            filter = selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedUiState())

    fun setFilter(kind: SavedKind?) {
        filter.value = kind
    }

    fun delete(id: Long) {
        viewModelScope.launch { container.savedRepository.delete(listOf(id)) }
    }

    fun updateNote(id: Long, note: String) {
        viewModelScope.launch { container.savedRepository.updateNote(id, note) }
    }

    fun deleteAll() {
        viewModelScope.launch { container.savedRepository.clearSaved() }
    }
}

fun SavedKind.label(): String = when (this) {
    SavedKind.EXPRESSION -> "Expression"
    SavedKind.FORMULA -> "Formula"
    SavedKind.CALCULATION -> "Calculation"
    SavedKind.DATASET -> "Dataset"
    SavedKind.EQUATION_SET -> "Equation set"
}
