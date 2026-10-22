package com.shohankhan.nexcalc.core.nav

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (Navigation Compose 2.8 with kotlinx.serialization).
 * Route classes carry only identifiers; no state is smuggled through string arguments.
 */
@Serializable
data object StandardRoute

@Serializable
data object ScientificRoute

@Serializable
data object FormulasRoute

@Serializable
data class FormulaDetailRoute(val formulaId: String)

@Serializable
data object QuickSolveRoute

@Serializable
data class QuickToolRoute(val toolId: String)

@Serializable
data object ConvertersRoute

@Serializable
data class ConverterCategoryRoute(val categoryId: String)

@Serializable
data object ToolsRoute

@Serializable
data object MatrixToolRoute

@Serializable
data object StatisticsToolRoute

@Serializable
data object EquationToolRoute

@Serializable
data object BaseToolRoute

@Serializable
data object ConstantsToolRoute

@Serializable
data object ResistorToolRoute

@Serializable
data object SavedToolRoute

@Serializable
data object HistoryRoute

@Serializable
data object SearchRoute

@Serializable
data object FavoritesRoute

@Serializable
data object SettingsRoute

@Serializable
data object AboutRoute

@Serializable
data object MoreRoute
