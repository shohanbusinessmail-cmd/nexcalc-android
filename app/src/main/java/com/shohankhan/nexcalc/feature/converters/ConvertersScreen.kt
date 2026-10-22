package com.shohankhan.nexcalc.feature.converters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.InfoBanner
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.NumberInputField
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.math.UnitDef
import com.shohankhan.nexcalc.math.UnitRegistry

@Composable
fun ConvertersScreen(initialCategoryId: String? = null) {
    val viewModel = containerViewModel { ConverterViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    val category = state.category

    LaunchedEffectOnce(initialCategoryId) {
        if (initialCategoryId != null && initialCategoryId != state.categoryId) {
            viewModel.selectCategory(initialCategoryId)
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.converters_title),
            subtitle = stringResource(R.string.converters_subtitle, UnitRegistry.categoryCount),
            actions = {
                IconAction(Icons.Default.Save, stringResource(R.string.action_save)) { viewModel.saveToHistory() }
            }
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.converter_category)) {
                DropdownSelector(
                    label = stringResource(R.string.converter_category),
                    selectedLabel = category?.name ?: "",
                    items = UnitRegistry.categories,
                    itemLabel = { it.name },
                    onSelect = { viewModel.selectCategory(it.id) }
                )
                category?.let {
                    Text(
                        it.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (category != null) {
                SectionCard(title = stringResource(R.string.converter_value)) {
                    NumberInputField(
                        value = state.input,
                        onValueChange = { viewModel.setInput(it) },
                        label = "${stringResource(R.string.converter_value)} (${state.fromSymbol})",
                        isError = state.error != null,
                        errorText = state.error
                    )
                    UnitSelectorRow(
                        fromLabel = stringResource(R.string.converter_from),
                        toLabel = stringResource(R.string.converter_to),
                        fromUnit = category.unit(state.fromUnitId),
                        toUnit = category.unit(state.toUnitId),
                        onSelectFrom = { viewModel.selectFrom(it.id) },
                        onSelectTo = { viewModel.selectTo(it.id) },
                        onSwap = { viewModel.swap() },
                        units = category.units
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.converter_result),
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (state.error != null) "—" else "${state.result} ${state.toSymbol}".trim(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = { copy("${state.result} ${state.toSymbol}".trim()) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.action_copy))
                            }
                        }
                        Text(
                            "${state.input} ${state.fromSymbol} = ${state.result} ${state.toSymbol}".trim(),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            stringResource(R.string.converter_precision),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                SectionCard(title = stringResource(R.string.converter_unit_notes)) {
                    UnitNote(category.unit(state.fromUnitId))
                    UnitNote(category.unit(state.toUnitId))
                    Text(
                        "Decimal (kB) and binary (KiB) byte units are separate; 1 KiB = 1024 B and 1 kB = 1000 B.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    InfoBanner(
                        "Currency conversion is intentionally not included: NEXCALC never fetches live rates.",
                        tone = com.shohankhan.nexcalc.core.design.BannerTone.INFO
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitNote(unit: UnitDef?) {
    if (unit == null) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "${unit.name} (${unit.symbol})",
            style = MaterialTheme.typography.titleSmall
        )
        if (unit.description.isNotBlank()) {
            Text(
                unit.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnitSelectorRow(
    fromLabel: String,
    toLabel: String,
    fromUnit: UnitDef?,
    toUnit: UnitDef?,
    units: List<UnitDef>,
    onSelectFrom: (UnitDef) -> Unit,
    onSelectTo: (UnitDef) -> Unit,
    onSwap: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f)) {
            DropdownSelector(
                label = fromLabel,
                selectedLabel = fromUnit?.let { "${it.name} (${it.symbol})" } ?: "",
                items = units,
                itemLabel = { "${it.name} (${it.symbol})" },
                onSelect = onSelectFrom
            )
        }
        IconButton(onClick = onSwap) {
            Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.action_swap))
        }
        Box(Modifier.weight(1f)) {
            DropdownSelector(
                label = toLabel,
                selectedLabel = toUnit?.let { "${it.name} (${it.symbol})" } ?: "",
                items = units,
                itemLabel = { "${it.name} (${it.symbol})" },
                onSelect = onSelectTo
            )
        }
    }
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    selectedLabel: String,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(
                selectedLabel.ifBlank { label },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        expanded = false
                        onSelect(item)
                    }
                )
            }
        }
    }
}

/** Runs [block] once per composition key, without re-running when state changes. */
@Composable
private fun LaunchedEffectOnce(key: Any?, block: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) { block() }
}
