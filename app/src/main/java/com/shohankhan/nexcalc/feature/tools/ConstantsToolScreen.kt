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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.SectionCard
import com.shohankhan.nexcalc.core.design.rememberCopyAction
import com.shohankhan.nexcalc.math.MathematicalIdentities
import com.shohankhan.nexcalc.math.NumberFormatOptions
import com.shohankhan.nexcalc.math.NumberFormatter
import com.shohankhan.nexcalc.math.PhysicalConstant
import com.shohankhan.nexcalc.math.PhysicalConstants

@Composable
fun ConstantsToolScreen(onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val constants = remember(query) {
        if (query.isBlank()) {
            PhysicalConstants.all
        } else {
            val needle = query.lowercase()
            PhysicalConstants.all.filter {
                it.name.lowercase().contains(needle) ||
                    it.symbol.lowercase().contains(needle) ||
                    it.description.lowercase().contains(needle) ||
                    it.id.contains(needle)
            }
        }
    }
    val copy = rememberCopyAction()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.tool_constants),
            subtitle = stringResource(R.string.tool_constants_desc),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            items(constants, key = { it.id }) { constant ->
                ConstantRow(constant) {
                    copy("${constant.name} ${constant.symbol} = ${constant.value} ${constant.unit}")
                }
            }
            item {
                SectionCard(title = stringResource(R.string.constants_identities)) {
                    MathematicalIdentities.all.forEach { identity ->
                        Column(Modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(identity.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                identity.statement,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                identity.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    "SI defining constants are exact by definition. Measured values use the CODATA 2022 release. " +
                        "No value is fetched at runtime.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConstantRow(constant: PhysicalConstant, onCopy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(constant.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        constant.symbol,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = if (constant.exact) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (constant.exact) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        stringResource(if (constant.exact) R.string.constants_exact else R.string.constants_measured),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconAction(Icons.Default.ContentCopy, stringResource(R.string.action_copy), onCopy)
            }
            Text(
                "${NumberFormatter.format(constant.value, NumberFormatOptions(decimalPrecision = 12, significantDigits = 12, useGrouping = false))} ${constant.unit}",
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                constant.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                constant.source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
