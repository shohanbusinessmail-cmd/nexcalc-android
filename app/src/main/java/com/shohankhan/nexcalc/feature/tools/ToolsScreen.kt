package com.shohankhan.nexcalc.feature.tools

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
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
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

private data class ToolEntry(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ToolsScreen(
    onOpenMatrix: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenEquations: () -> Unit,
    onOpenBases: () -> Unit,
    onOpenConstants: () -> Unit,
    onOpenResistor: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    val tools = listOf(
        ToolEntry(stringResource(R.string.tool_matrix), stringResource(R.string.tool_matrix_desc), Icons.Default.GridOn, onOpenMatrix),
        ToolEntry(stringResource(R.string.tool_statistics), stringResource(R.string.tool_statistics_desc), Icons.Default.Calculate, onOpenStatistics),
        ToolEntry(stringResource(R.string.tool_equations), stringResource(R.string.tool_equations_desc), Icons.Default.Functions, onOpenEquations),
        ToolEntry(stringResource(R.string.tool_bases), stringResource(R.string.tool_bases_desc), Icons.Default.Functions, onOpenBases),
        ToolEntry(stringResource(R.string.tool_constants), stringResource(R.string.tool_constants_desc), Icons.Default.Info, onOpenConstants),
        ToolEntry(stringResource(R.string.tool_resistor), stringResource(R.string.tool_resistor_desc), Icons.Default.Straighten, onOpenResistor),
        ToolEntry(stringResource(R.string.tool_saved), stringResource(R.string.tool_saved_desc), Icons.Default.Save, onOpenSaved),
        ToolEntry(stringResource(R.string.nav_favorites), stringResource(R.string.favorites_subtitle), Icons.Default.Star, onOpenFavorites)
    )
    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tools_title),
            subtitle = stringResource(R.string.tools_subtitle)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools, key = { it.title }) { entry ->
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
                            Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                entry.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
