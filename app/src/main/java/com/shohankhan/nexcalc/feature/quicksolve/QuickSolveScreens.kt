package com.shohankhan.nexcalc.feature.quicksolve

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.SavedRepository
import com.shohankhan.nexcalc.core.design.BannerTone
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.InfoBanner
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.LabeledValue
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.NumberInputField
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.feature.favorites.FavoriteButton
import com.shohankhan.nexcalc.math.QuickTool
import com.shohankhan.nexcalc.math.QuickToolRegistry

@Composable
fun QuickSolveScreen(onOpenTool: (String) -> Unit, onOpenFavorites: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val tools = remember(query, categoryName) {
        val searched = if (query.isBlank()) QuickToolRegistry.all else QuickToolRegistry.search(query)
        if (categoryName == null) searched else searched.filter { it.category.name == categoryName }
    }
    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.quicksolve_title),
            subtitle = stringResource(R.string.quicksolve_subtitle),
            actions = {
                IconAction(Icons.Default.Star, stringResource(R.string.nav_favorites), onOpenFavorites)
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoryName == null,
                            onClick = { categoryName = null },
                            label = { Text(stringResource(R.string.formula_all_categories)) }
                        )
                    }
                    items(QuickToolRegistry.categories) { category ->
                        FilterChip(
                            selected = categoryName == category.name,
                            onClick = { categoryName = if (categoryName == category.name) null else category.name },
                            label = { Text(category.label) }
                        )
                    }
                }
            }
            if (tools.isEmpty()) {
                item { EmptyState(stringResource(R.string.state_no_results)) }
            } else {
                items(tools, key = { it.id }) { tool ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTool(tool.id) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tool.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                tool.category.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickToolScreen(toolId: String, onBack: () -> Unit) {
    val viewModel = containerViewModel { QuickToolDetailViewModel(it, toolId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tool: QuickTool? = viewModel.tool
    val copy = rememberCopyAction()

    if (tool == null) {
        Column(Modifier.fillMaxSize()) {
            NexTopBar(title = stringResource(R.string.quicksolve_title), onBack = onBack)
            EmptyState(stringResource(R.string.tool_not_found))
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = tool.name,
            subtitle = tool.category.label,
            onBack = onBack,
            actions = {
                FavoriteButton(
                    kind = SavedRepository.KIND_TOOL,
                    id = tool.id,
                    label = tool.name,
                    subtitle = tool.category.label
                )
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(description = tool.description) {
                    Text(
                        stringResource(R.string.tool_inputs),
                        style = MaterialTheme.typography.titleMedium
                    )
                    state.inputs.forEach { input ->
                        val definition = tool.inputs.first { it.id == input.id }
                        NumberInputField(
                            value = input.value,
                            onValueChange = { viewModel.setInput(input.id, it) },
                            label = definition.label,
                            isError = input.error != null,
                            errorText = input.error,
                            placeholder = definition.placeholder.ifBlank { null },
                            singleLine = true
                        )
                    }
                    if (state.failure != null) {
                        InfoBanner(state.failure ?: "", tone = BannerTone.ERROR)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { viewModel.calculate() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_calculate))
                        }
                        OutlinedButton(
                            onClick = { viewModel.saveToWorkspace() },
                            enabled = state.result != null
                        ) { Text(stringResource(R.string.action_save)) }
                    }
                    if (state.saved) {
                        Text(
                            stringResource(R.string.formula_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            state.result?.let { result ->
                item {
                    SectionCard(title = stringResource(R.string.tool_result)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                result.headline,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy)) {
                                copy(result.headline + "\n" + result.outputs.joinToString("\n") { "${it.label}: ${it.value}" })
                            }
                        }
                        result.outputs.forEach { output ->
                            LabeledValue(output.label, output.value)
                        }
                    }
                }
                if (result.steps.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.tool_steps)) {
                            result.steps.forEach { step ->
                                Text("• $step", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (result.warnings.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.tool_warnings)) {
                            result.warnings.forEach { warning ->
                                InfoBanner(warning, tone = BannerTone.WARNING)
                            }
                        }
                    }
                }
            }
        }
    }
}
