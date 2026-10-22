package com.shohankhan.nexcalc.core.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shohankhan.nexcalc.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided")
}

/**
 * Creates a ViewModel with the app container, scoped to the current navigation back stack entry so
 * each screen keeps its own instance and survives configuration changes.
 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(noinline create: (AppContainer) -> VM): VM {
    val container = LocalAppContainer.current
    val factory = remember(container) {
        viewModelFactory {
            initializer { create(container) }
        }
    }
    return viewModel(factory = factory)
}
