package com.shohankhan.nexcalc.feature.tools

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.shohankhan.nexcalc.core.data.local.SavedKind
import com.shohankhan.nexcalc.core.data.local.SavedRecord
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.core.nav.containerViewModel

@Composable
fun SavedWorkspaceScreen(onBack: () -> Unit, onOpenFavorites: () -> Unit) {
    val viewModel = containerViewModel { SavedWorkspaceViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copy = rememberCopyAction()
    var editing by remember { mutableStateOf<SavedRecord?>(null) }
    var noteText by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_saved),
            subtitle = "${state.items.size} items · ${state.favorites.size} favorites",
            onBack = onBack,
            actions = {
                IconAction(Icons.Default.Star, stringResource(R.string.nav_favorites), onClick = onOpenFavorites)
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.filter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text(stringResource(R.string.saved_filter_all)) }
                        )
                    }
                    items(SavedKind.entries) { kind ->
                        FilterChip(
                            selected = state.filter == kind,
                            onClick = { viewModel.setFilter(if (state.filter == kind) null else kind) },
                            label = { Text(kind.label()) }
                        )
                    }
                }
            }
            if (state.items.isEmpty()) {
                item { EmptyState(stringResource(R.string.state_nothing_saved)) }
            } else {
                items(state.items, key = { it.id }) { record ->
                    SavedCard(
                        record = record,
                        onCopy = { copy(record.payload) },
                        onEditNote = {
                            editing = record
                            noteText = record.note
                        },
                        onDelete = { viewModel.delete(record.id) }
                    )
                }
            }
        }
    }

    val target = editing
    if (target != null) {
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(R.string.saved_notes)) },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.saved_add_note)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateNote(target.id, noteText)
                    editing = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@Composable
private fun SavedCard(record: SavedRecord, onCopy: () -> Unit, onEditNote: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        record.savedKind.label(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy), onClick = onCopy)
                IconAction(Icons.Default.Edit, stringResource(R.string.saved_notes), onClick = onEditNote)
                IconAction(Icons.Default.Delete, stringResource(R.string.action_delete), onClick = onDelete)
            }
            Text(
                record.payload,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (record.note.isNotBlank()) {
                Text(record.note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
