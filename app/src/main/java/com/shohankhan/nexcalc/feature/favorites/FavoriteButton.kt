package com.shohankhan.nexcalc.feature.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.data.SavedRepository
import com.shohankhan.nexcalc.core.nav.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * Favorites are stored by key (`FORMULA:hero-area`, `TOOL:gcd-lcm`, `UNIT:length`) so the list can
 * be rebuilt offline without duplicating catalogue data.
 */
@Composable
fun FavoriteButton(kind: String, id: String, label: String, subtitle: String) {
    val container = LocalAppContainer.current
    val key = SavedRepository.favoriteKey(kind, id)
    val isFavorite by container.savedRepository.observeIsFavorite(key).collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    IconButton(onClick = {
        scope.launch { container.savedRepository.toggleFavorite(kind, id, label, subtitle, !isFavorite) }
    }) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = stringResource(R.string.cd_favorite),
            tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
