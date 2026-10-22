package com.shohankhan.nexcalc.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.BannerTone
import com.shohankhan.nexcalc.core.design.InfoBanner
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.NumberInputField
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel

@Composable
fun MatrixToolScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { MatrixToolViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_matrix),
            subtitle = stringResource(R.string.tool_matrix_desc),
            onBack = onBack,
            actions = {
                IconAction(Icons.Default.Refresh, stringResource(R.string.action_reset)) { viewModel.reset() }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(
                    title = stringResource(R.string.matrix_matrix_a),
                    description = "${state.aDimensions} · ${stringResource(R.string.matrix_edit_hint)}"
                ) {
                    NumberInputField(
                        value = state.aText,
                        onValueChange = { viewModel.setA(it) },
                        label = "A",
                        singleLine = false,
                        minLines = 3
                    )
                    Text(
                        stringResource(R.string.matrix_matrix_b),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(state.bDimensions, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    NumberInputField(
                        value = state.bText,
                        onValueChange = { viewModel.setB(it) },
                        label = "B",
                        singleLine = false,
                        minLines = 3
                    )
                    Text(
                        stringResource(R.string.matrix_vector_b),
                        style = MaterialTheme.typography.titleSmall
                    )
                    NumberInputField(
                        value = state.vectorText,
                        onValueChange = { viewModel.setVector(it) },
                        label = "b",
                        singleLine = false,
                        minLines = 1
                    )
                }
            }
            item {
                SectionCard(title = "Operations") {
                    MatrixOperation.entries.chunked(3).forEach { rowOperations ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            rowOperations.forEach { operation ->
                                OutlinedButton(
                                    onClick = { viewModel.compute(operation) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(operation.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            repeat(3 - rowOperations.size) {
                                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            if (state.error != null) {
                item { InfoBanner(state.error ?: "", tone = BannerTone.ERROR) }
            }
            state.result?.let { result ->
                item {
                    SectionCard(title = stringResource(R.string.matrix_result)) {
                        Row {
                            Text(
                                result,
                                modifier = Modifier.weight(1f),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy)) { copy(result) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { viewModel.saveToWorkspace() }) {
                                Text(stringResource(R.string.action_save))
                            }
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
            }
            item {
                InfoBanner(
                    "Matrices are limited to 12 × 12. Determinants use LU decomposition with partial pivoting; " +
                        "systems with a pivot below 1e-12 are reported as singular instead of returning an unreliable answer.",
                    tone = BannerTone.INFO
                )
            }
        }
    }
}
