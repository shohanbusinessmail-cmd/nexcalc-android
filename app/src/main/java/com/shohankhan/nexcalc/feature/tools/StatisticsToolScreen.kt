package com.shohankhan.nexcalc.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun StatisticsToolScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { StatisticsToolViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_statistics),
            subtitle = stringResource(R.string.tool_statistics_desc),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.stats_dataset)) {
                    NumberInputField(
                        value = state.datasetText,
                        onValueChange = { viewModel.setDataset(it) },
                        label = stringResource(R.string.stats_dataset),
                        singleLine = false,
                        minLines = 3,
                        placeholder = stringResource(R.string.stats_dataset_hint)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { viewModel.describe() }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_calculate))
                        }
                        OutlinedButton(onClick = { viewModel.saveDataset() }, enabled = state.summary.isNotEmpty()) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                    NumberInputField(
                        value = state.savedName,
                        onValueChange = { viewModel.setSavedName(it) },
                        label = stringResource(R.string.stats_save_name),
                        singleLine = true
                    )
                    if (state.saved) {
                        Text(
                            stringResource(R.string.stats_saved),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            if (state.error != null) {
                item { InfoBanner(state.error ?: "", tone = BannerTone.ERROR) }
            }
            if (state.summary.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.stats_summary)) {
                        state.summary.forEach { (label, value) ->
                            LabeledValue(label, value)
                        }
                    }
                }
                item {
                    SectionCard(title = stringResource(R.string.stats_frequency)) {
                        Row {
                            Text(
                                stringResource(R.string.stats_frequency),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_export)) {
                                copy(viewModel.csvText())
                            }
                        }
                        state.frequency.forEach { (value, count) ->
                            LabeledValue(value, count)
                        }
                    }
                }
                item {
                    SectionCard(title = stringResource(R.string.stats_regression)) {
                        Text(
                            stringResource(R.string.stats_paired),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NumberInputField(
                            value = state.xText,
                            onValueChange = { viewModel.setX(it) },
                            label = stringResource(R.string.stats_x_values),
                            singleLine = false
                        )
                        NumberInputField(
                            value = state.yText,
                            onValueChange = { viewModel.setY(it) },
                            label = stringResource(R.string.stats_y_values),
                            singleLine = false
                        )
                        Button(onClick = { viewModel.regress() }) {
                            Text(stringResource(R.string.stats_regression))
                        }
                        if (state.regressionError != null) {
                            InfoBanner(state.regressionError ?: "", tone = BannerTone.ERROR)
                        }
                        state.regression.forEach { (label, value) ->
                            LabeledValue(label, value)
                        }
                    }
                }
                item {
                    Text(
                        "Quartiles and percentiles use linear interpolation between closest ranks (R type 7). " +
                            "Sample and population variance are reported separately; n = 1 has no sample variance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
