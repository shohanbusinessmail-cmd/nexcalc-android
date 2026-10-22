package com.shohankhan.nexcalc.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.local.HistoryRecord
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.LocalSnackbarHost
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.LocalAppContainer
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.feature.calculator.HistorySource
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { HistoryViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by LocalAppContainer.current.settings.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    val snackbar = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    var sourceMenuOpen by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.history_deleted)

    fun offerUndo(removed: List<HistoryRecord>) {
        if (removed.isEmpty() || snackbar == null) return
        scope.launch {
            val result = snackbar.showSnackbar(
                message = deletedMessage,
                actionLabel = "Undo",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(removed)
        }
    }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.history_title),
            subtitle = stringResource(R.string.history_subtitle),
            onBack = onBack,
            actions = {
                if (state.selectionMode) {
                    IconAction(Icons.Default.Delete, stringResource(R.string.action_delete_selected, state.selectedCount)) {
                        scope.launch { offerUndo(viewModel.deleteSelected()) }
                    }
                    IconAction(Icons.Default.Check, stringResource(R.string.action_cancel)) {
                        viewModel.setSelectionMode(false)
                    }
                } else {
                    IconAction(Icons.Default.Check, stringResource(R.string.action_delete)) {
                        viewModel.setSelectionMode(true)
                    }
                    IconAction(Icons.Default.Delete, stringResource(R.string.action_clear)) { confirmClear = true }
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.setQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.history_search_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { sourceMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                if (state.source == "ALL") stringResource(R.string.history_all_sources)
                                else HistorySource.label(state.source)
                            )
                        }
                    }
                    DropdownMenu(expanded = sourceMenuOpen, onDismissRequest = { sourceMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history_all_sources)) },
                            onClick = {
                                sourceMenuOpen = false
                                viewModel.setSource("ALL")
                            }
                        )
                        state.sources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(HistorySource.label(source)) },
                                onClick = {
                                    sourceMenuOpen = false
                                    viewModel.setSource(source)
                                }
                            )
                        }
                    }
                }
            }
            val today = LocalDate.now()
            if (state.groups.isEmpty()) {
                item { EmptyState(stringResource(R.string.state_empty_history)) }
            } else {
                state.groups.forEach { group ->
                    item(key = "day-${group.epochDay}") {
                        Text(
                            labelForDay(group.epochDay, today),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(group.records, key = { it.id }) { record ->
                        HistoryRow(
                            record = record,
                            selectionMode = state.selectionMode,
                            selected = record.id in state.selected,
                            onToggle = { viewModel.toggleSelection(record.id) },
                            onCopy = { copy("${record.expression} = ${record.result}") },
                            onDelete = { scope.launch { offerUndo(viewModel.deleteOne(record)) } }
                        )
                    }
                }
            }
            item {
                Text(
                    if (settings.historyRetentionDays < 0) {
                        stringResource(R.string.history_retention_never)
                    } else {
                        stringResource(R.string.history_retention_notice, settings.retentionLabel)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch { offerUndo(viewModel.clearAll()) }
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    record: HistoryRecord,
    selectionMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    record.expression,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(record.result, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${HistorySource.label(record.source)} · ${timeLabel(record.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!selectionMode) {
                IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy), onClick = onCopy)
                IconAction(Icons.Default.Delete, stringResource(R.string.action_delete), onClick = onDelete)
            }
        }
    }
}

@Composable
private fun labelForDay(epochDay: Long, today: LocalDate): String {
    val date = LocalDate.ofEpochDay(epochDay)
    return when (epochDay) {
        today.toEpochDay() -> stringResource(R.string.history_today)
        today.minusDays(1).toEpochDay() -> stringResource(R.string.history_yesterday)
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
}

private fun timeLabel(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH))
