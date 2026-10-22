package com.shohankhan.nexcalc.di

import android.content.Context
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.data.DataTransfer
import com.shohankhan.nexcalc.core.data.HistoryRepository
import com.shohankhan.nexcalc.core.data.PreferencesRepository
import com.shohankhan.nexcalc.core.data.SavedRepository
import com.shohankhan.nexcalc.core.data.local.NexCalcDatabase
import com.shohankhan.nexcalc.core.search.SearchIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Manual dependency container.
 *
 * NEXCALC uses one explicit, testable container instead of a DI framework: the graph is tiny, the
 * wiring stays visible, and unit tests can build repositories with in-memory databases. The
 * container is created once by [com.shohankhan.nexcalc.NexCalcApplication].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: NexCalcDatabase = NexCalcDatabase.get(appContext)

    val preferences: PreferencesRepository = PreferencesRepository(appContext)

    val historyRepository: HistoryRepository = HistoryRepository(database.historyDao(), preferences)

    val savedRepository: SavedRepository = SavedRepository(database.savedDao(), database.favoriteDao())

    val dataTransfer: DataTransfer = DataTransfer(database, preferences)

    val searchIndex: SearchIndex = SearchIndex()

    /** Defaults are shown until DataStore emits, which keeps startup free of a loading flash. */
    val settings: StateFlow<AppSettings> = preferences.settings
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    fun startMaintenance() {
        scope.launch {
            runCatching { historyRepository.cleanupNow() }
        }
    }
}
