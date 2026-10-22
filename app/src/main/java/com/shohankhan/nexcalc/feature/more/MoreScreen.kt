package com.shohankhan.nexcalc.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.search.SearchIndex
import com.shohankhan.nexcalc.math.FormulaRegistry
import com.shohankhan.nexcalc.math.PhysicalConstants
import com.shohankhan.nexcalc.math.QuickToolRegistry
import com.shohankhan.nexcalc.math.UnitRegistry

private data class MoreEntry(val label: String, val description: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
fun MoreScreen(
    onOpenQuickSolve: () -> Unit,
    onOpenConverters: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val solving = listOf(
        MoreEntry(stringResource(R.string.nav_quicksolve), stringResource(R.string.quicksolve_subtitle), Icons.Default.Functions, onOpenQuickSolve),
        MoreEntry(stringResource(R.string.nav_converters), stringResource(R.string.converters_title), Icons.Default.SwapHoriz, onOpenConverters),
        MoreEntry(stringResource(R.string.nav_tools), stringResource(R.string.tools_subtitle), Icons.Default.Calculate, onOpenTools)
    )
    val personal = listOf(
        MoreEntry(stringResource(R.string.nav_history), stringResource(R.string.history_subtitle), Icons.Default.History, onOpenHistory),
        MoreEntry(stringResource(R.string.nav_search), stringResource(R.string.search_hint), Icons.Default.Search, onOpenSearch),
        MoreEntry(stringResource(R.string.nav_favorites), stringResource(R.string.favorites_subtitle), Icons.Default.Star, onOpenFavorites),
        MoreEntry(stringResource(R.string.tool_saved), stringResource(R.string.tool_saved_desc), Icons.Default.Functions, onOpenSaved),
        MoreEntry(stringResource(R.string.nav_settings), stringResource(R.string.settings_title), Icons.Default.Settings, onOpenSettings),
        MoreEntry(stringResource(R.string.nav_about), stringResource(R.string.app_tagline), Icons.Default.Info, onOpenAbout)
    )

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.more_title),
            subtitle = stringResource(R.string.more_subtitle)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SectionCard(title = "Offline catalogue") {
                    Text(
                        "${FormulaRegistry.count} formulas · ${QuickToolRegistry.count} Quick Solve tools · " +
                            "${UnitRegistry.categoryCount} unit categories (${UnitRegistry.unitCount} units) · " +
                            "${PhysicalConstants.count} constants · ${SearchIndex().entryCount} searchable entries",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.more_problem_solving),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(solving, key = { it.label }) { entry -> MoreCard(entry) }
            item {
                Text(
                    stringResource(R.string.more_personal),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(personal, key = { it.label }) { entry -> MoreCard(entry) }
        }
    }
}

@Composable
private fun MoreCard(entry: MoreEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = entry.onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(entry.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
