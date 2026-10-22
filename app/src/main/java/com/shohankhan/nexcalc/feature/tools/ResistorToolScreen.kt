package com.shohankhan.nexcalc.feature.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.BannerTone
import com.shohankhan.nexcalc.core.design.InfoBanner
import com.shohankhan.nexcalc.core.design.LabeledValue
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.ResistorCalculator
import com.shohankhan.nexcalc.math.ResistorColor

@Composable
fun ResistorToolScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { ResistorToolViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_resistor),
            subtitle = stringResource(R.string.tool_resistor_desc),
            onBack = onBack,
            actions = {
                com.shohankhan.nexcalc.core.design.IconAction(
                    Icons.Default.Save,
                    stringResource(R.string.action_save)
                ) { viewModel.saveToHistory() }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.resistor_bands)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5, 6).forEach { count ->
                            FilterChip(
                                selected = state.bandCount == count,
                                onClick = { viewModel.setBandCount(count) },
                                label = { Text("$count") }
                            )
                        }
                    }
                    state.bands.forEachIndexed { index, color ->
                        BandSelector(
                            role = roleLabel(state.bandCount, index),
                            selected = color,
                            options = optionsFor(state.bandCount, index),
                            onSelect = { viewModel.setBand(index, it) }
                        )
                    }
                }
            }
            if (state.error != null) {
                item { InfoBanner(state.error ?: "", tone = BannerTone.ERROR) }
            }
            state.result?.let { result ->
                item {
                    SectionCard(title = stringResource(R.string.resistor_value)) {
                        Text(
                            result.formattedResistance,
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        LabeledValue(
                            stringResource(R.string.resistor_tolerance),
                            result.tolerancePercent?.let { "±${NumberFormatter.format(it, NumberFormatOptions(decimalPrecision = 3, useGrouping = false))}%" } ?: "unspecified"
                        )
                        if (result.minimumOhms != null && result.maximumOhms != null) {
                            LabeledValue(
                                stringResource(R.string.resistor_range),
                                "${ResistorCalculator.formatResistance(result.minimumOhms)} – ${ResistorCalculator.formatResistance(result.maximumOhms)}"
                            )
                        }
                        result.temperatureCoefficientPpm?.let {
                            LabeledValue(stringResource(R.string.resistor_tempco), "$it ppm/K")
                        }
                        Text(
                            result.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                InfoBanner(
                    "Tolerance and temperature bands are read from the standard E-series colour table. " +
                        "A gold or silver multiplier means 0.1× or 0.01×; the 'None' band is only valid as a tolerance band.",
                    tone = BannerTone.INFO
                )
            }
        }
    }
}

private fun roleLabel(bandCount: Int, index: Int): String {
    val digitCount = bandCount - 2
    return when {
        index < digitCount -> "Digit ${index + 1}"
        index == digitCount -> "Multiplier"
        index == digitCount + 1 -> "Tolerance"
        else -> "Temperature coefficient"
    }
}

private fun optionsFor(bandCount: Int, index: Int): List<ResistorColor> {
    val digitCount = bandCount - 2
    return when {
        index < digitCount -> ResistorColor.entries.filter { it.digit != null }
        index == digitCount -> ResistorColor.entries.filter { it.multiplier != null }
        index == digitCount + 1 -> ResistorColor.entries.filter { it.tolerancePercent != null }
        else -> ResistorColor.entries.filter { it.temperatureCoefficientPpm != null }
    }
}

@Composable
private fun BandSelector(
    role: String,
    selected: ResistorColor,
    options: List<ResistorColor>,
    onSelect: (ResistorColor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(role, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .size(16.dp)
                        .background(swatch(selected), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
                Text(
                    selected.label,
                    modifier = Modifier.padding(start = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { color ->
                    DropdownMenuItem(
                        text = { Text(color.label) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .background(swatch(color), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(color)
                        }
                    )
                }
            }
        }
    }
}

private fun swatch(color: ResistorColor): Color = when (color) {
    ResistorColor.BLACK -> Color(0xFF111111)
    ResistorColor.BROWN -> Color(0xFF6D4C41)
    ResistorColor.RED -> Color(0xFFD32F2F)
    ResistorColor.ORANGE -> Color(0xFFF57C00)
    ResistorColor.YELLOW -> Color(0xFFFBC02D)
    ResistorColor.GREEN -> Color(0xFF2E7D32)
    ResistorColor.BLUE -> Color(0xFF1565C0)
    ResistorColor.VIOLET -> Color(0xFF7B1FA2)
    ResistorColor.GREY -> Color(0xFF757575)
    ResistorColor.WHITE -> Color(0xFFF5F5F5)
    ResistorColor.GOLD -> Color(0xFFC9A227)
    ResistorColor.SILVER -> Color(0xFFB0BEC5)
    ResistorColor.NONE -> Color(0xFF000000).copy(alpha = 0.15f)
}
