package com.shohankhan.nexcalc.feature.formulas

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.SavedRepository
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
import com.shohankhan.nexcalc.math.FormulaRegistry

@Composable
fun FormulaDetailScreen(formulaId: String, onBack: () -> Unit, onOpenFormula: (String) -> Unit) {
    val viewModel = containerViewModel { FormulaDetailViewModel(it, formulaId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formula = viewModel.formula
    val copy = rememberCopyAction()

    if (formula == null) {
        Column(Modifier.fillMaxSize()) {
            NexTopBar(title = stringResource(R.string.formulas_title), onBack = onBack)
            EmptyState(stringResource(R.string.formula_not_found))
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = formula.name,
            subtitle = "${formula.category.label} · ${formula.subcategory}",
            onBack = onBack,
            actions = {
                FavoriteButton(
                    kind = SavedRepository.KIND_FORMULA,
                    id = formula.id,
                    label = formula.name,
                    subtitle = formula.category.label
                )
                IconAction(Icons.Default.Refresh, stringResource(R.string.action_reset)) { viewModel.reset() }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.formula_notation)) {
                    Text(formula.notation, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
                    Text(
                        formula.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (formula.assumptions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.formula_assumptions),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        formula.assumptions.forEach { assumption ->
                            Text("• $assumption", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.formula_inputs)) {
                    state.inputs.forEach { input ->
                        val definition = formula.inputs.first { it.id == input.id }
                        val label = if (definition.unit.isNotBlank()) "${definition.label} (${definition.unit})" else definition.label
                        NumberInputField(
                            value = input.value,
                            onValueChange = { viewModel.setInput(input.id, it) },
                            label = label,
                            isError = input.error != null,
                            errorText = input.error,
                            placeholder = definition.help.ifBlank { null },
                            singleLine = true
                        )
                    }
                    if (state.failure != null) {
                        InfoBanner(state.failure ?: "", tone = com.shohankhan.nexcalc.core.design.BannerTone.ERROR)
                    }
                    if (state.hint != null) {
                        InfoBanner(state.hint ?: "", tone = com.shohankhan.nexcalc.core.design.BannerTone.INFO)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.calculate() },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.action_calculate)) }
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
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.formula_result),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { copy(result.values.joinToString("\n") { "${it.label}: ${it.value} ${it.unit}".trim() }) }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_copy))
                                }
                            }
                            result.values.forEach { value ->
                                LabeledValue(
                                    label = value.label,
                                    value = "${value.value} ${value.unit}".trim(),
                                    emphasize = value.primary
                                )
                            }
                        }
                    }
                }
                if (result.steps.isNotEmpty()) {
                    item {
                        SectionCard(title = stringResource(R.string.formula_steps)) {
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
                                InfoBanner(warning, tone = com.shohankhan.nexcalc.core.design.BannerTone.WARNING)
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.formula_reference)) {
                    Text(
                        formula.reference.ifBlank { "NEXCALC mathematical engine" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        stringResource(R.string.formula_precision),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(formula.precisionNote, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (formula.related.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.formula_related)) {
                        formula.related.forEach { id ->
                            val related = FormulaRegistry.find(id) ?: return@forEach
                            Text(
                                related.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFormula(related.id) }
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
