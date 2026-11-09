package com.joulearc.app.features.usage

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.joulearc.app.core.PageTitle
import com.joulearc.app.core.insetsPadding
import com.joulearc.app.core.PillToggle
import com.joulearc.app.core.formatClock
import com.joulearc.app.core.formatDateTime
import com.joulearc.app.core.formatDuration
import com.joulearc.app.data.AppUsage
import com.joulearc.app.data.LevelSample
import com.joulearc.app.data.dailyDrain
import com.joulearc.app.data.dayHourlyLevels
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
    selectedDay: Long?,
    selectedHour: Long?,
    onSelectDay: (Long?) -> Unit,
    onSelectHour: (Long?) -> Unit,
    onAppClick: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val options = listOf(stringResource(R.string.apps_window_24h), stringResource(R.string.apps_window_14d))
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = insetsPadding(16.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title") { PageTitle(stringResource(R.string.usage_per_app)) }
        item(key = "toggle") {
            PillToggle(
                options = options,
                selected = if (windowDays == 1) 0 else 1,
                onSelect = { onWindowChange(if (it == 0) 1 else 14) },
            )
        }
        item(key = "chart") { ChartSection(samples, windowDays, selectedDay, selectedHour, onSelectDay, onSelectHour) }
        selectionLabel(windowDays, selectedDay, selectedHour)?.let { label ->
            item(key = "showing") {
                Text(
                    text = stringResource(R.string.usage_showing, label),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
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
            else -> items(report.apps, key = { it.packageName }) { app -> AppRow(app, report) { onAppClick(app.packageName) } }
        }
    }
}

private fun hourRange(startMs: Long): String {
    val format = SimpleDateFormat("HH:mm", Locale.getDefault())
    return format.format(Date(startMs)) + "-" + format.format(Date(startMs + 3_600_000L))
}

private fun dayLabel(startMs: Long): String =
    SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(startMs))

/** Text for what the list below is limited to (an hour or a day), or null when it shows everything. */
private fun selectionLabel(windowDays: Int, day: Long?, hour: Long?): String? = when {
    hour != null -> if (windowDays == 1) hourRange(hour) else dayLabel(hour) + ", " + hourRange(hour)
    day != null -> dayLabel(day)
    else -> null
}

@Composable
private fun ChartSection(
    samples: List<LevelSample>,
    windowDays: Int,
    selectedDay: Long?,
    selectedHour: Long?,
    onSelectDay: (Long?) -> Unit,
    onSelectHour: (Long?) -> Unit,
) {
    if (samples.isEmpty()) {
        Text(stringResource(R.string.usage_no_history), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val now = remember(samples) { System.currentTimeMillis() }
    val hourFormat = remember { SimpleDateFormat("H", Locale.getDefault()) }
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
            HourChart(bars, hourFormat, selectedHour, onSelectHour)
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
                selected = days.indexOfFirst { it.startMs == selectedDay }.takeIf { it >= 0 },
                onSelect = { index -> onSelectDay(index?.let { days[it].startMs }) },
                hints = days.map { dayLabel(it.startMs) },
            )
            if (selectedDay != null) {
                val bars = remember(samples, selectedDay) { dayHourlyLevels(samples, selectedDay, now) }
                HourChart(bars, hourFormat, selectedHour, onSelectHour)
            }
        }
    }
}

@Composable
private fun HourChart(
    bars: List<com.joulearc.app.data.HourBar>,
    hourFormat: SimpleDateFormat,
    selectedHour: Long?,
    onSelectHour: (Long?) -> Unit,
) {
    BarChart(
        values = bars.map { it.level?.toFloat() },
        maxValue = 100f,
        yLabels = listOf("100%", "50%", "0%"),
        xLabels = bars.mapIndexed { i, b -> if (i % 3 == 0) hourFormat.format(Date(b.startMs)) else "" },
        highlighted = bars.indices.filter { bars[it].plugged }.toSet(),
        selected = bars.indexOfFirst { it.startMs == selectedHour }.takeIf { it >= 0 },
        onSelect = { index -> onSelectHour(index?.let { bars[it].startMs }) },
        hints = bars.map { hourRange(it.startMs) },
    )
}

@Composable
private fun AppRow(app: AppUsage, report: UsageReport, onClick: () -> Unit) {
    val percent = report.drainedPercent?.let { app.share * it }
    val trailing = when {
        percent == null -> formatDuration(app.foregroundMs + app.backgroundMs)
        percent < 1f -> stringResource(R.string.usage_percent_less)
        else -> stringResource(R.string.apps_share, percent.roundToInt())
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
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
