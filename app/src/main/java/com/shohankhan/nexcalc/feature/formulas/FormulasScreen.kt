package com.shohankhan.nexcalc.feature.formulas

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.math.Formula
import com.shohankhan.nexcalc.math.FormulaRegistry

@Composable
fun FormulasScreen(onOpenFormula: (String) -> Unit, onOpenFavorites: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = FormulaRegistry.categories
    val formulas = remember(query, categoryName) {
        val searched = if (query.isBlank()) FormulaRegistry.all else FormulaRegistry.search(query)
        val filtered = if (categoryName == null) searched else searched.filter { it.category.name == categoryName }
        filtered
    }
    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.formulas_title),
            subtitle = stringResource(
                R.string.formulas_subtitle,
                FormulaRegistry.count,
                FormulaRegistry.categories.size
            ),
            actions = {
                IconAction(Icons.Default.Star, stringResource(R.string.nav_favorites), onOpenFavorites)
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.formulas_search_hint)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = categoryName == null,
                            onClick = { categoryName = null },
                            label = { Text(stringResource(R.string.formula_all_categories)) }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = categoryName == category.name,
                            onClick = {
                                categoryName = if (categoryName == category.name) null else category.name
                            },
                            label = { Text(category.label) }
                        )
                    }
                }
            }
            if (formulas.isEmpty()) {
                item { EmptyState(stringResource(R.string.state_no_results)) }
            } else {
                items(formulas, key = { it.id }) { formula ->
                    FormulaRow(formula) { onOpenFormula(formula.id) }
                }
            }
        }
    }
}

@Composable
private fun FormulaRow(formula: Formula, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(formula.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formula.category.label} · ${formula.subcategory}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formula.notation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
