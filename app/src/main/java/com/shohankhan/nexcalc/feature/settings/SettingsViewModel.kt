package com.shohankhan.nexcalc.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.data.ImportOutcome
import com.shohankhan.nexcalc.di.AppContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settings

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { container.preferences.update(transform) }
    }

    suspend fun cleanupNow(): Int = container.historyRepository.cleanupNow()

    suspend fun exportJson(): String = container.dataTransfer.exportJson()

    suspend fun importJson(text: String): ImportOutcome = container.dataTransfer.importJson(text)

    fun deleteAllData() {
        viewModelScope.launch {
            container.historyRepository.clear()
            container.savedRepository.clearSaved()
            container.savedRepository.clearFavorites()
            container.preferences.reset()
        }
    }
}
