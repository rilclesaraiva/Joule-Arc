package com.joulearc.app.features.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.formatDuration
import com.joulearc.app.data.AppUsage
import kotlin.math.roundToInt

@Composable
fun AppRankingScreen(
    apps: List<AppUsage>?,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.apps_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = windowDays == 1,
                        onClick = { onWindowChange(1) },
                        label = { Text(stringResource(R.string.apps_window_24h)) },
                    )
                    FilterChip(
                        selected = windowDays == 7,
                        onClick = { onWindowChange(7) },
                        label = { Text(stringResource(R.string.apps_window_7d)) },
                    )
                }
                Text(
                    text = stringResource(R.string.apps_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        when {
            apps == null -> item(key = "loading") {
                Text(stringResource(R.string.apps_loading), style = MaterialTheme.typography.bodyMedium)
            }
            apps.isEmpty() -> item(key = "empty") {
                Text(stringResource(R.string.apps_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                val top = apps.first().share
                items(apps, key = { it.packageName }) { app -> AppRow(app, top) }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, topShare: Float) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.apps_share, (app.share * 100).roundToInt()),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            LinearProgressIndicator(
                progress = { if (topShare > 0f) app.share / topShare else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(
                    R.string.apps_row,
                    formatDuration(app.foregroundMs),
                    formatDuration(app.backgroundMs),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
