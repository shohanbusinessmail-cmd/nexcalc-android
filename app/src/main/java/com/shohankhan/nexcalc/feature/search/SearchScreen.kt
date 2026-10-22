package com.shohankhan.nexcalc.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.nav.containerViewModel
import com.shohankhan.nexcalc.core.search.SearchGroup
import com.shohankhan.nexcalc.core.search.SearchHit
import com.shohankhan.nexcalc.core.search.SearchTarget

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (SearchTarget) -> Unit) {
    val viewModel = containerViewModel { SearchViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(title = stringResource(R.string.search_title), onBack = onBack)
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.setQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        if (state.query.isBlank()) {
            EmptyState(stringResource(R.string.search_prompt))
            return@Column
        }
        if (state.hits.isEmpty()) {
            EmptyState(stringResource(R.string.search_no_results, state.query))
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val grouped = state.hits.groupBy { it.group }
            SearchGroup.entries.forEach { group ->
                val hits = grouped[group] ?: return@forEach
                item(key = "group-${group.name}") {
                    Text(
                        groupLabel(group),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                items(hits, key = { it.key }) { hit ->
                    SearchRow(hit) { onOpen(hit.target) }
                }
            }
        }
    }
}

@Composable
private fun groupLabel(group: SearchGroup): String = when (group) {
    SearchGroup.FUNCTION -> stringResource(R.string.search_group_functions)
    SearchGroup.FORMULA -> stringResource(R.string.search_group_formulas)
    SearchGroup.TOOL -> stringResource(R.string.search_group_tools)
    SearchGroup.UNIT -> stringResource(R.string.search_group_units)
    SearchGroup.CONSTANT -> stringResource(R.string.search_group_constants)
    SearchGroup.APP_TOOL -> stringResource(R.string.tools_title)
    SearchGroup.HISTORY -> stringResource(R.string.search_group_history)
    SearchGroup.SAVED -> stringResource(R.string.search_group_saved)
    SearchGroup.FAVORITE -> stringResource(R.string.nav_favorites)
}

@Composable
private fun SearchRow(hit: SearchHit, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(hit.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            hit.subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
