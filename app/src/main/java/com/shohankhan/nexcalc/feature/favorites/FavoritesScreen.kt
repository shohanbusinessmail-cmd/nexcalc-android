package com.shohankhan.nexcalc.feature.favorites

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.local.FavoriteRecord
import com.shohankhan.nexcalc.core.design.EmptyState
import com.shohankhan.nexcalc.core.design.IconAction
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.nav.containerViewModel

@Composable
fun FavoritesScreen(onBack: () -> Unit, onOpen: (FavoriteRecord) -> Unit) {
    val viewModel = containerViewModel { FavoritesViewModel(it) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        NexTopBar(
            title = stringResource(R.string.favorites_title),
            subtitle = stringResource(R.string.favorites_subtitle),
            onBack = onBack
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.favorites.isEmpty()) {
                item { EmptyState(stringResource(R.string.state_no_favorites)) }
            } else {
                items(state.favorites, key = { it.key }) { favorite ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(favorite) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    favorite.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    favorite.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconAction(
                                icon = Icons.Default.Star,
                                contentDescription = stringResource(R.string.action_remove)
                            ) { viewModel.remove(favorite.key) }
                        }
                    }
                }
            }
        }
    }
}
