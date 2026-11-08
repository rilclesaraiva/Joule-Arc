package com.joulearc.app.features.usage

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.BarChart
import com.joulearc.app.core.PillToggle
import com.joulearc.app.core.formatClock
import com.joulearc.app.core.formatDateTime
import com.joulearc.app.core.formatDuration
import com.joulearc.app.data.AppUsage
import com.joulearc.app.data.LevelSample
import com.joulearc.app.data.dailyDrain
import com.joulearc.app.data.hourlyLevels
import com.joulearc.app.data.lastFullCharge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/** Ranked apps for a window, with icons and the battery percentage drained in that window. */
data class UsageReport(
    val apps: List<AppUsage>,
    val icons: Map<String, ImageBitmap>,
    /** Percentage of battery drained while unplugged in the window, or null without level history. */
    val drainedPercent: Float?,
    val fromMs: Long,
    /** True when the window was shortened because the level history is newer than the window. */
    val clipped: Boolean,
)

@Composable
fun BatteryUsageScreen(
    report: UsageReport?,
    samples: List<LevelSample>,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(stringResource(R.string.apps_window_24h), stringResource(R.string.apps_window_14d))
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "toggle") {
            PillToggle(
                options = options,
                selected = if (windowDays == 1) 0 else 1,
                onSelect = { onWindowChange(if (it == 0) 1 else 14) },
            )
        }
        item(key = "chart") { ChartSection(samples, windowDays) }
        if (report?.clipped == true) {
            item(key = "since") {
                Text(
                    text = stringResource(R.string.usage_since, formatDateTime(report.fromMs)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.usage_per_app),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }
                Text(text = stringResource(R.string.apps_note), style = MaterialTheme.typography.bodySmall)
            }
        }
        when {
            report == null -> item(key = "loading") {
                Text(stringResource(R.string.apps_loading), style = MaterialTheme.typography.bodyMedium)
            }
            report.apps.isEmpty() -> item(key = "empty") {
                Text(stringResource(R.string.apps_empty), style = MaterialTheme.typography.bodyMedium)
            }
            else -> items(report.apps, key = { it.packageName }) { app -> AppRow(app, report) }
        }
    }
}

@Composable
private fun ChartSection(samples: List<LevelSample>, windowDays: Int) {
    if (samples.isEmpty()) {
        Text(stringResource(R.string.usage_no_history), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val now = remember(samples) { System.currentTimeMillis() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (windowDays == 1) {
            lastFullCharge(samples)?.let {
                Column {
                    Text(stringResource(R.string.usage_last_full), style = MaterialTheme.typography.bodyLarge)
                    Text(formatClock(it), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Text(
                text = stringResource(R.string.usage_chart_level),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val bars = remember(samples) { hourlyLevels(samples, now) }
            val hourFormat = remember { SimpleDateFormat("H", Locale.getDefault()) }
            BarChart(
                values = bars.map { it.level?.toFloat() },
                maxValue = 100f,
                yLabels = listOf("100%", "50%", "0%"),
                xLabels = bars.mapIndexed { i, b -> if (i % 3 == 0) hourFormat.format(Date(b.startMs)) else "" },
                highlighted = bars.indices.filter { bars[it].plugged }.toSet(),
            )
        } else {
            Text(
                text = stringResource(R.string.usage_chart_usage),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            val days = remember(samples) { dailyDrain(samples, now) }
            val top = max(100f, ceil((days.maxOfOrNull { it.drainPercent ?: 0f } ?: 0f) / 50f) * 50f)
            val weekdayFormat = remember { SimpleDateFormat("EEEEE", Locale.getDefault()) }
            BarChart(
                values = days.map { it.drainPercent },
                maxValue = top,
                yLabels = listOf("${top.toInt()}%", "${(top / 2).toInt()}%", "0%"),
                xLabels = days.map { weekdayFormat.format(Date(it.startMs)).uppercase(Locale.getDefault()) },
                highlighted = emptySet(),
            )
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, report: UsageReport) {
    val percent = report.drainedPercent?.let { app.share * it }
    val trailing = when {
        percent == null -> formatDuration(app.foregroundMs + app.backgroundMs)
        percent < 1f -> stringResource(R.string.usage_percent_less)
        else -> stringResource(R.string.apps_share, percent.roundToInt())
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val icon = report.icons[app.packageName]
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(0.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
        Text(text = trailing, style = MaterialTheme.typography.titleMedium)
    }
}
