package com.shohankhan.nexcalc.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FavoritesUiState(val favorites: List<FavoriteRecord> = emptyList())

class FavoritesViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<FavoritesUiState> = container.savedRepository.observeFavorites()
        .map { FavoritesUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavoritesUiState())

    fun remove(key: String) {
        viewModelScope.launch { container.savedRepository.removeFavorite(key) }
    }
}
