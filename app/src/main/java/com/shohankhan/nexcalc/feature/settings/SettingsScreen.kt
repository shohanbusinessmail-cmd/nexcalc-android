package com.shohankhan.nexcalc.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.AccentColor
import com.shohankhan.nexcalc.core.data.AppSettings
import com.shohankhan.nexcalc.core.data.ImportOutcome
import com.shohankhan.nexcalc.core.data.ThemeMode
import com.shohankhan.nexcalc.core.design.LocalSnackbarHost
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.math.AngleMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { SettingsViewModel(it) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    val exportFailed = stringResource(R.string.settings_export_failed)
    val exportSuccess = stringResource(R.string.settings_export_success)
    val importFailed = stringResource(R.string.settings_import_failed)
    val deleteMessage = stringResource(R.string.settings_deleted)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = viewModel.exportJson()
            val written = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    true
                }.getOrDefault(false)
            }
            snackbar?.showSnackbar(if (written) exportSuccess else exportFailed)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text == null) {
                snackbar?.showSnackbar(importFailed)
            } else {
                when (val outcome = viewModel.importJson(text)) {
                    is ImportOutcome.Success -> snackbar?.showSnackbar(context.getString(R.string.settings_import_success, outcome.records))
                    is ImportOutcome.Failure -> snackbar?.showSnackbar(outcome.message)
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.settings_appearance)) {
                SettingLabel(stringResource(R.string.settings_theme))
                ChoiceRow(
                    options = listOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                    ),
                    selected = settings.themeMode,
                    onSelect = { mode -> viewModel.update { it.copy(themeMode = mode) } }
                )
                SettingLabel(stringResource(R.string.settings_accent))
                ChoiceRow(
                    options = listOf(
                        AccentColor.BLUE to stringResource(R.string.settings_accent_blue),
                        AccentColor.TEAL to stringResource(R.string.settings_accent_teal),
                        AccentColor.VIOLET to stringResource(R.string.settings_accent_violet)
                    ),
                    selected = settings.accent,
                    onSelect = { accent -> viewModel.update { it.copy(accent = accent) } }
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_dynamic_color),
                    description = stringResource(R.string.settings_dynamic_color_hint),
                    checked = settings.dynamicColor,
                    onCheckedChange = { enabled -> viewModel.update { it.copy(dynamicColor = enabled) } }
                )
            }

            SectionCard(title = stringResource(R.string.settings_numbers)) {
                SliderSetting(
                    label = stringResource(R.string.settings_decimal_precision),
                    value = settings.decimalPrecision.toFloat(),
                    range = 0f..15f,
                    steps = 14,
                    onChange = { value -> viewModel.update { it.copy(decimalPrecision = value.toInt()) } }
                )
                SliderSetting(
                    label = stringResource(R.string.settings_significant_digits),
                    value = settings.significantDigits.toFloat(),
                    range = 1f..15f,
                    steps = 13,
                    onChange = { value -> viewModel.update { it.copy(significantDigits = value.toInt()) } }
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_grouping),
                    checked = settings.useGrouping,
                    onCheckedChange = { enabled -> viewModel.update { it.copy(useGrouping = enabled) } }
                )
                SettingLabel(stringResource(R.string.settings_decimal_separator))
                ChoiceRow(
                    options = listOf('.' to ".", ',' to ","),
                    selected = settings.decimalSeparator,
                    onSelect = { separator -> viewModel.update { it.copy(decimalSeparator = separator) } }
                )
                SettingLabel(stringResource(R.string.settings_grouping_separator))
                ChoiceRow(
                    options = listOf(',' to ",", ' ' to "space", '_' to "_"),
                    selected = settings.groupingSeparator,
                    onSelect = { separator -> viewModel.update { it.copy(groupingSeparator = separator) } }
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_trailing_zeros),
                    checked = settings.showTrailingZeros,
                    onCheckedChange = { enabled -> viewModel.update { it.copy(showTrailingZeros = enabled) } }
                )
            }

            SectionCard(title = stringResource(R.string.settings_calculator)) {
                SettingLabel(stringResource(R.string.settings_angle_mode))
                ChoiceRow(
                    options = listOf(
                        AngleMode.DEGREES to stringResource(R.string.angle_mode_degrees),
                        AngleMode.RADIANS to stringResource(R.string.angle_mode_radians),
                        AngleMode.GRADIANS to stringResource(R.string.angle_mode_gradians)
                    ),
                    selected = settings.angleMode,
                    onSelect = { mode -> viewModel.update { it.copy(angleMode = mode) } }
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_haptics),
                    checked = settings.haptics,
                    onCheckedChange = { enabled -> viewModel.update { it.copy(haptics = enabled) } }
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_animations),
                    description = stringResource(R.string.settings_animations_hint),
                    checked = settings.animations,
                    onCheckedChange = { enabled -> viewModel.update { it.copy(animations = enabled) } }
                )
            }

            SectionCard(title = stringResource(R.string.settings_history)) {
                SettingLabel(stringResource(R.string.settings_retention))
                ChoiceRow(
                    options = listOf(
                        1 to stringResource(R.string.settings_retention_1),
                        7 to stringResource(R.string.settings_retention_7),
                        30 to stringResource(R.string.settings_retention_30),
                        90 to stringResource(R.string.settings_retention_90),
                        -1 to stringResource(R.string.settings_retention_never)
                    ),
                    selected = settings.historyRetentionDays,
                    onSelect = { days -> viewModel.update { it.copy(historyRetentionDays = days) } }
                )
                Text(
                    if (settings.historyRetentionDays < 0) {
                        stringResource(R.string.history_retention_never)
                    } else {
                        stringResource(R.string.history_retention_notice, settings.retentionLabel)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = {
                    scope.launch {
                        val removed = viewModel.cleanupNow()
                        snackbar?.showSnackbar(context.getString(R.string.settings_cleanup_done, removed))
                    }
                }) { Text(stringResource(R.string.settings_cleanup_now)) }
            }

            SectionCard(title = stringResource(R.string.settings_data)) {
                Text(
                    stringResource(R.string.settings_backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { exportLauncher.launch("nexcalc-backup.json") }) {
                        Text(stringResource(R.string.settings_export))
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) {
                        Text(stringResource(R.string.settings_import))
                    }
                }
                OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_delete_data), color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Exports contain your history, saved work, favorites and settings as plain JSON. " +
                        "Nothing is uploaded; the file is written only where you choose.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.settings_delete_data_title)) },
            text = { Text(stringResource(R.string.settings_delete_data_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllData()
                    scope.launch { snackbar?.showSnackbar(deleteMessage) }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun <T> ChoiceRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(label, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value.toInt().toString(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
