package com.shohankhan.nexcalc.core.nav

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.core.design.LocalHapticsEnabled
import com.shohankhan.nexcalc.core.design.LocalNumberFormat
import com.shohankhan.nexcalc.core.design.LocalSettings
import com.shohankhan.nexcalc.core.design.LocalSnackbarHost
import com.shohankhan.nexcalc.core.design.NexCalcTheme
import com.shohankhan.nexcalc.core.search.SearchTarget
import com.shohankhan.nexcalc.di.AppContainer
import com.shohankhan.nexcalc.feature.about.AboutScreen
import com.shohankhan.nexcalc.feature.calculator.CalculatorScreen
import com.shohankhan.nexcalc.feature.converters.ConvertersScreen
import com.shohankhan.nexcalc.feature.favorites.FavoritesScreen
import com.shohankhan.nexcalc.feature.formulas.FormulaDetailScreen
import com.shohankhan.nexcalc.feature.formulas.FormulasScreen
import com.shohankhan.nexcalc.feature.history.HistoryScreen
import com.shohankhan.nexcalc.feature.more.MoreScreen
import com.shohankhan.nexcalc.feature.quicksolve.QuickSolveScreen
import com.shohankhan.nexcalc.feature.quicksolve.QuickToolScreen
import com.shohankhan.nexcalc.feature.search.SearchScreen
import com.shohankhan.nexcalc.feature.settings.SettingsScreen
import com.shohankhan.nexcalc.feature.tools.BaseToolScreen
import com.shohankhan.nexcalc.feature.tools.ConstantsToolScreen
import com.shohankhan.nexcalc.feature.tools.EquationToolScreen
import com.shohankhan.nexcalc.feature.tools.MatrixToolScreen
import com.shohankhan.nexcalc.feature.tools.ResistorToolScreen
import com.shohankhan.nexcalc.feature.tools.SavedWorkspaceScreen
import com.shohankhan.nexcalc.feature.tools.StatisticsToolScreen
import com.shohankhan.nexcalc.feature.tools.ToolsScreen

private data class NavItem(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selected: (NavDestination?) -> Boolean,
    val navigate: (NavController) -> Unit
)

private inline fun <reified T : Any> NavDestination?.isOn(): Boolean =
    this?.hierarchy?.any { it.hasRoute<T>() } == true

private inline fun <reified T : Any> NavController.go(route: T) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private val bottomDestinations = listOf(
    NavItem(R.string.nav_standard, Icons.Default.Calculate, { it.isOn<StandardRoute>() }) { it.go(StandardRoute) },
    NavItem(R.string.nav_scientific, Icons.Default.Science, { it.isOn<ScientificRoute>() }) { it.go(ScientificRoute) },
    NavItem(
        R.string.nav_formulas,
        Icons.Default.Functions,
        { destination -> destination.isOn<FormulasRoute>() || destination.isOn<FormulaDetailRoute>() }
    ) { it.go(FormulasRoute) },
    NavItem(
        R.string.nav_more,
        Icons.Default.MoreHoriz,
        { destination ->
            destination.isOn<MoreRoute>() ||
                destination.isOn<QuickSolveRoute>() ||
                destination.isOn<QuickToolRoute>() ||
                destination.isOn<ConvertersRoute>() ||
                destination.isOn<ConverterCategoryRoute>() ||
                destination.isOn<ToolsRoute>() ||
                destination.isOn<MatrixToolRoute>() ||
                destination.isOn<StatisticsToolRoute>() ||
                destination.isOn<EquationToolRoute>() ||
                destination.isOn<BaseToolRoute>() ||
                destination.isOn<ConstantsToolRoute>() ||
                destination.isOn<ResistorToolRoute>() ||
                destination.isOn<SavedToolRoute>() ||
                destination.isOn<HistoryRoute>() ||
                destination.isOn<SearchRoute>() ||
                destination.isOn<FavoritesRoute>() ||
                destination.isOn<SettingsRoute>() ||
                destination.isOn<AboutRoute>()
        }
    ) { it.go(MoreRoute) }
)

private val railDestinations = listOf(
    bottomDestinations[0],
    bottomDestinations[1],
    bottomDestinations[2],
    NavItem(R.string.nav_quicksolve, Icons.Default.Functions, { it.isOn<QuickSolveRoute>() || it.isOn<QuickToolRoute>() }) { it.go(QuickSolveRoute) },
    NavItem(R.string.nav_converters, Icons.Default.SwapHoriz, { it.isOn<ConvertersRoute>() || it.isOn<ConverterCategoryRoute>() }) { it.go(ConvertersRoute) },
    NavItem(R.string.nav_tools, Icons.Default.Calculate, { it.isOn<ToolsRoute>() }) { it.go(ToolsRoute) }
)

@Composable
fun NexCalcApp(container: AppContainer) {
    val settings by container.settings.collectAsStateWithLifecycle()
    NexCalcTheme(settings) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination
        CompositionLocalProvider(
            LocalAppContainer provides container,
            LocalSettings provides settings,
            LocalNumberFormat provides settings.numberFormat,
            LocalHapticsEnabled provides settings.haptics,
            LocalSnackbarHost provides snackbarHostState
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 600.dp
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = { if (!useRail) BottomBar(navController, destination) }
                ) { padding ->
                    Row(Modifier.fillMaxSize().padding(padding)) {
                        if (useRail) SideRail(navController, destination)
                        AppNavHost(navController, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavController, destination: NavDestination?) {
    NavigationBar {
        bottomDestinations.forEach { item ->
            NavigationBarItem(
                selected = item.selected(destination),
                onClick = { item.navigate(navController) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(stringResource(item.labelRes), textAlign = TextAlign.Center) }
            )
        }
    }
}

@Composable
private fun SideRail(navController: NavController, destination: NavDestination?) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(
            Modifier
                .fillMaxHeight()
                .width(96.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                railDestinations.forEach { item ->
                    RailButton(item, destination, navController)
                }
            }
            RailIcon(Icons.Default.History, R.string.nav_history) { navController.go(HistoryRoute) }
            RailIcon(Icons.Default.Settings, R.string.nav_settings) { navController.go(SettingsRoute) }
        }
    }
}

@Composable
private fun RailButton(item: NavItem, destination: NavDestination?, navController: NavController) {
    val selected = item.selected(destination)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .clickable { item.navigate(navController) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RailIcon(icon: ImageVector, labelRes: Int, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = stringResource(labelRes), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AppNavHost(navController: NavHostController, modifier: Modifier) {
    NavHost(navController = navController, startDestination = StandardRoute, modifier = modifier) {
        composable<StandardRoute> { CalculatorScreen(scientific = false) }
        composable<ScientificRoute> { CalculatorScreen(scientific = true) }
        composable<FormulasRoute> {
            FormulasScreen(
                onOpenFormula = { navController.navigate(FormulaDetailRoute(it)) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) }
            )
        }
        composable<FormulaDetailRoute> { entry ->
            val route = entry.toRoute<FormulaDetailRoute>()
            FormulaDetailScreen(
                formulaId = route.formulaId,
                onBack = { navController.popBackStack() },
                onOpenFormula = { navController.navigate(FormulaDetailRoute(it)) }
            )
        }
        composable<QuickSolveRoute> {
            QuickSolveScreen(
                onOpenTool = { navController.navigate(QuickToolRoute(it)) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) }
            )
        }
        composable<QuickToolRoute> { entry ->
            val route = entry.toRoute<QuickToolRoute>()
            QuickToolScreen(toolId = route.toolId, onBack = { navController.popBackStack() })
        }
        composable<ConvertersRoute> { ConvertersScreen() }
        composable<ConverterCategoryRoute> { entry ->
            val route = entry.toRoute<ConverterCategoryRoute>()
            ConvertersScreen(initialCategoryId = route.categoryId)
        }
        composable<ToolsRoute> {
            ToolsScreen(
                onOpenMatrix = { navController.navigate(MatrixToolRoute) },
                onOpenStatistics = { navController.navigate(StatisticsToolRoute) },
                onOpenEquations = { navController.navigate(EquationToolRoute) },
                onOpenBases = { navController.navigate(BaseToolRoute) },
                onOpenConstants = { navController.navigate(ConstantsToolRoute) },
                onOpenResistor = { navController.navigate(ResistorToolRoute) },
                onOpenSaved = { navController.navigate(SavedToolRoute) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) }
            )
        }
        composable<MatrixToolRoute> { MatrixToolScreen(onBack = { navController.popBackStack() }) }
        composable<StatisticsToolRoute> { StatisticsToolScreen(onBack = { navController.popBackStack() }) }
        composable<EquationToolRoute> { EquationToolScreen(onBack = { navController.popBackStack() }) }
        composable<BaseToolRoute> { BaseToolScreen(onBack = { navController.popBackStack() }) }
        composable<ConstantsToolRoute> { ConstantsToolScreen(onBack = { navController.popBackStack() }) }
        composable<ResistorToolRoute> { ResistorToolScreen(onBack = { navController.popBackStack() }) }
        composable<SavedToolRoute> {
            SavedWorkspaceScreen(
                onBack = { navController.popBackStack() },
                onOpenFavorites = { navController.navigate(FavoritesRoute) }
            )
        }
        composable<HistoryRoute> { HistoryScreen(onBack = { navController.popBackStack() }) }
        composable<SearchRoute> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpen = { target -> openTarget(navController, target) }
            )
        }
        composable<FavoritesRoute> {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onOpen = { favorite -> openFavorite(navController, favorite) }
            )
        }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable<AboutRoute> { AboutScreen(onBack = { navController.popBackStack() }) }
        composable<MoreRoute> {
            MoreScreen(
                onOpenQuickSolve = { navController.navigate(QuickSolveRoute) },
                onOpenConverters = { navController.navigate(ConvertersRoute) },
                onOpenTools = { navController.navigate(ToolsRoute) },
                onOpenHistory = { navController.navigate(HistoryRoute) },
                onOpenSearch = { navController.navigate(SearchRoute) },
                onOpenFavorites = { navController.navigate(FavoritesRoute) },
                onOpenSaved = { navController.navigate(SavedToolRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenAbout = { navController.navigate(AboutRoute) }
            )
        }
    }
}

private fun openTarget(navController: NavHostController, target: SearchTarget) {
    when (target) {
        is SearchTarget.Formula -> navController.navigate(FormulaDetailRoute(target.formulaId))
        is SearchTarget.QuickTool -> navController.navigate(QuickToolRoute(target.toolId))
        is SearchTarget.UnitCategory -> navController.navigate(ConverterCategoryRoute(target.categoryId))
        is SearchTarget.Screen -> when (target.screenId) {
            "scientific" -> navController.navigate(ScientificRoute)
            "matrix" -> navController.navigate(MatrixToolRoute)
            "statistics" -> navController.navigate(StatisticsToolRoute)
            "equations" -> navController.navigate(EquationToolRoute)
            "bases" -> navController.navigate(BaseToolRoute)
            "constants" -> navController.navigate(ConstantsToolRoute)
            "resistor" -> navController.navigate(ResistorToolRoute)
            "saved" -> navController.navigate(SavedToolRoute)
            "history" -> navController.navigate(HistoryRoute)
            "favorites" -> navController.navigate(FavoritesRoute)
            else -> Unit
        }
    }
}

private fun openFavorite(navController: NavHostController, favorite: FavoriteRecord) {
    val id = favorite.key.substringAfter(':', "")
    when (favorite.kind) {
        com.shohankhan.nexcalc.core.data.SavedRepository.KIND_FORMULA -> navController.navigate(FormulaDetailRoute(id))
        com.shohankhan.nexcalc.core.data.SavedRepository.KIND_TOOL -> navController.navigate(QuickToolRoute(id))
        com.shohankhan.nexcalc.core.data.SavedRepository.KIND_UNIT -> navController.navigate(ConverterCategoryRoute(id))
        else -> Unit
    }
}
