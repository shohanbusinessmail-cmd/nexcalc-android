package com.shohankhan.nexcalc.feature.about

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.shohankhan.nexcalc.BuildConfig
import com.shohankhan.nexcalc.R
import com.shohankhan.nexcalc.core.design.LabeledValue
import com.shohankhan.nexcalc.core.design.NexTopBar
import com.shohankhan.nexcalc.core.design.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        NexTopBar(title = stringResource(R.string.about_title), onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard(title = stringResource(R.string.app_name), description = stringResource(R.string.app_tagline)) {
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                LabeledValue(stringResource(R.string.about_owner), "Shohan Khan")
                LabeledValue(stringResource(R.string.about_contact), "helloiamshohan@gmail.com")
                LabeledValue(stringResource(R.string.about_package), BuildConfig.APPLICATION_ID)
                LabeledValue(stringResource(R.string.about_build_type), BuildConfig.BUILD_TYPE)
                LabeledValue(stringResource(R.string.about_legal), BuildConfig.SIGNING_KIND)
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = android.net.Uri.parse("mailto:helloiamshohan@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "NEXCALC feedback")
                    }
                    runCatching { context.startActivity(intent) }
                }) { Text(stringResource(R.string.about_email_owner)) }
            }
            SectionCard(title = stringResource(R.string.about_offline)) {
                Text(stringResource(R.string.about_offline_body), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_storage)) {
                Text(stringResource(R.string.about_storage_body), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_privacy)) {
                Text(stringResource(R.string.about_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
            SectionCard(title = stringResource(R.string.about_licenses)) {
                Text(stringResource(R.string.about_licenses_body), style = MaterialTheme.typography.bodySmall)
            }
            SectionCard(title = stringResource(R.string.about_legal)) {
                Text(stringResource(R.string.about_legal_body), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.about_built_with),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                stringResource(R.string.about_developer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
