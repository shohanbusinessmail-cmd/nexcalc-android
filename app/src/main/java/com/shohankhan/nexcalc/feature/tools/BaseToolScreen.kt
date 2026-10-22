package com.shohankhan.nexcalc.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.BannerTone
import com.shohankhan.nexcalc.core.design.InfoBanner
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.LabeledValue
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.NumberInputField
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel

@Composable
fun BaseToolScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { BaseToolViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    val bases = listOf(2 to "Binary", 8 to "Octal", 10 to "Decimal", 16 to "Hexadecimal")

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_bases),
            subtitle = stringResource(R.string.tool_bases_desc),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.bases_input)) {
                    NumberInputField(
                        value = state.valueText,
                        onValueChange = { viewModel.setValue(it) },
                        label = "${stringResource(R.string.bases_input)} (${stringResource(R.string.bases_base)} ${state.base})",
                        isError = state.error != null,
                        errorText = state.error
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(bases) { (base, label) ->
                            FilterChip(
                                selected = state.base == base,
                                onClick = { viewModel.setBase(base) },
                                label = { Text("$label ($base)") }
                            )
                        }
                    }
                    state.outputs.forEach { (label, value) ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            LabeledValue(label, value, Modifier.weight(1f))
                            IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy)) { copy("$label: $value") }
                        }
                    }
                    Text(
                        "Values wider than 64 bits are rejected; signed results use two's complement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                SectionCard(title = stringResource(R.string.bases_operands)) {
                    NumberInputField(
                        value = state.aText,
                        onValueChange = { viewModel.setA(it) },
                        label = stringResource(R.string.bases_operand_a),
                        singleLine = true
                    )
                    NumberInputField(
                        value = state.bText,
                        onValueChange = { viewModel.setB(it) },
                        label = stringResource(R.string.bases_operand_b),
                        singleLine = true
                    )
                    NumberInputField(
                        value = state.shiftText,
                        onValueChange = { viewModel.setShift(it) },
                        label = stringResource(R.string.bases_shift),
                        singleLine = true
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BitwiseOperation.entries) { operation ->
                            FilterChip(
                                selected = state.operation == operation,
                                onClick = { viewModel.setOperation(operation) },
                                label = { Text(operation.label) }
                            )
                        }
                    }
                    Button(onClick = { viewModel.applyBitwise() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.bases_operation))
                    }
                    if (state.bitwiseError != null) {
                        InfoBanner(state.bitwiseError ?: "", tone = BannerTone.ERROR)
                    }
                    state.bitwise.forEach { (label, value) ->
                        LabeledValue(label, value)
                    }
                }
            }
            item {
                Text(
                    "Bitwise operations act on 64-bit signed integers using Kotlin's defined shift semantics; " +
                        "shifts outside 0–63 bits are rejected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}
