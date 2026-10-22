package com.shohankhan.nexcalc.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.math.NumberFormatter

@Composable
fun EquationToolScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { EquationToolViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_equations),
            subtitle = stringResource(R.string.tool_equations_desc),
            onBack = onBack,
            actions = {
                IconAction(Icons.Default.Save, stringResource(R.string.equations_save_set)) { viewModel.saveEquationSet() }
            }
        )
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionCard(title = stringResource(R.string.equations_system)) {
                    Text(
                        stringResource(R.string.equations_matrix_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    NumberInputField(
                        value = state.matrixText,
                        onValueChange = { viewModel.setMatrix(it) },
                        label = "A",
                        singleLine = false,
                        minLines = 2
                    )
                    NumberInputField(
                        value = state.rhsText,
                        onValueChange = { viewModel.setRhs(it) },
                        label = "b",
                        singleLine = false,
                        minLines = 2
                    )
                    Button(onClick = { viewModel.solveSystem() }) { Text(stringResource(R.string.matrix_solve)) }
                    if (state.systemError != null) {
                        InfoBanner(state.systemError ?: "", tone = BannerTone.ERROR)
                    }
                    state.systemResult?.let {
                        Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.equations_polynomial)) {
                    NumberInputField(
                        value = state.coefficientsText,
                        onValueChange = { viewModel.setCoefficients(it) },
                        label = stringResource(R.string.equations_coefficients),
                        singleLine = true
                    )
                    Button(onClick = { viewModel.findRoots() }) { Text(stringResource(R.string.equations_polynomial)) }
                    if (state.rootsError != null) {
                        InfoBanner(state.rootsError ?: "", tone = BannerTone.ERROR)
                    }
                    state.roots.forEach {
                        Text(it, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.roots.isNotEmpty()) {
                        Text(
                            "Roots are polished with Newton steps. Repeated roots may be reported with a small imaginary residue.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.equations_numeric)) {
                    NumberInputField(
                        value = state.functionText,
                        onValueChange = { viewModel.setFunction(it) },
                        label = stringResource(R.string.equations_function),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        NumericMethod.entries.forEach { method ->
                            FilterChip(
                                selected = state.method == method,
                                onClick = { viewModel.setMethod(method) },
                                label = { Text(method.label) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberInputField(
                            value = state.lowerText,
                            onValueChange = { viewModel.setLower(it) },
                            label = stringResource(R.string.equations_lower),
                            modifier = Modifier.weight(1f)
                        )
                        NumberInputField(
                            value = state.upperText,
                            onValueChange = { viewModel.setUpper(it) },
                            label = stringResource(R.string.equations_upper),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    NumberInputField(
                        value = state.toleranceText,
                        onValueChange = { viewModel.setTolerance(it) },
                        label = stringResource(R.string.equations_tolerance),
                        singleLine = true
                    )
                    Button(onClick = { viewModel.solveNumeric() }) { Text(stringResource(R.string.equations_root)) }
                    if (state.numericError != null) {
                        InfoBanner(state.numericError ?: "", tone = BannerTone.ERROR)
                    }
                    state.numeric?.let { solution ->
                        if (solution.converged) {
                            LabeledValue(stringResource(R.string.equations_root), solution.root?.let { NumberFormatter.format(it) } ?: "—", emphasize = true)
                        } else {
                            InfoBanner(solution.message, tone = BannerTone.ERROR)
                        }
                        LabeledValue(stringResource(R.string.equations_iterations), solution.iterations.toString())
                        solution.residual?.let { LabeledValue(stringResource(R.string.equations_residual), NumberFormatter.format(it)) }
                        LabeledValue(
                            stringResource(R.string.equations_converged),
                            if (solution.converged) "Yes" else "No"
                        )
                        Text(solution.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            item {
                InfoBanner(
                    "Iterative methods stop at the stated tolerance or report failure. " +
                        "Newton uses a numerical derivative; bracketing methods require a sign change.",
                    tone = BannerTone.INFO
                )
            }
        }
    }
}
