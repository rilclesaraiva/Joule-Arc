package com.joulearc.app.features.usage

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.PageTitle
import com.joulearc.app.core.PillToggle
import com.joulearc.app.core.formatDuration
import com.joulearc.app.core.insetsPadding
import com.joulearc.app.data.AppUsage
import com.joulearc.app.data.BACKGROUND_WEIGHT
import kotlin.math.roundToInt

/** Battery use of one app: total, active (on screen) and background, in time and estimated percent. */
@Composable
fun AppUsageScreen(
    packageName: String,
    report: UsageReport?,
    windowDays: Int,
    onWindowChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = report?.apps?.firstOrNull { it.packageName == packageName }
    val options = listOf(stringResource(R.string.apps_window_24h), stringResource(R.string.apps_window_14d))
    LazyColumn(
        modifier = modifier,
        contentPadding = insetsPadding(16.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "title") { PageTitle(app?.label ?: packageName) }
        item(key = "card") {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    val icon = report?.icons?.get(packageName)
                    if (icon != null) {
                        Image(icon, null, Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
                    }
                    Text(app?.label ?: packageName, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        item(key = "toggle") {
            PillToggle(
                options = options,
                selected = if (windowDays == 1) 0 else 1,
                onSelect = { onWindowChange(if (it == 0) 1 else 14) },
            )
        }
        item(key = "body") {
            when {
                report == null -> Text(stringResource(R.string.apps_loading), style = MaterialTheme.typography.bodyMedium)
                app == null -> Text(stringResource(R.string.usage_app_none), style = MaterialTheme.typography.bodyMedium)
                else -> Breakdown(app, report)
            }
        }
        item(key = "note") {
            Text(
                stringResource(R.string.usage_app_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Breakdown(app: AppUsage, report: UsageReport) {
    val fgWeight = app.foregroundMs.toDouble()
    val bgWeight = app.backgroundMs * BACKGROUND_WEIGHT
    val weightSum = fgWeight + bgWeight
    val totalPercent = report.drainedPercent?.let { app.share * it }
    val activePercent = totalPercent?.let { if (weightSum > 0) (it * fgWeight / weightSum).toFloat() else 0f }
    val backgroundPercent = totalPercent?.let { it - (activePercent ?: 0f) }
    @Composable fun percentText(p: Float?): String = when {
        p == null -> "—"
        p < 1f -> stringResource(R.string.usage_percent_less)
        else -> stringResource(R.string.apps_share, p.roundToInt())
    }
    val activeColor = Color(0xFF2E7DF6)
    val backgroundColor = Color(0xFFF59E0B)
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.usage_app_total, formatDuration(app.foregroundMs + app.backgroundMs), percentText(totalPercent)),
            style = MaterialTheme.typography.titleMedium,
        )
        // The bar is a share of the whole battery (like Android's): active, then background, then the rest.
        val timeTotal = (app.foregroundMs + app.backgroundMs).coerceAtLeast(1L).toFloat()
        val activeFraction = activePercent?.let { it / 100f } ?: (app.foregroundMs / timeTotal)
        val backgroundFraction = backgroundPercent?.let { it / 100f } ?: (app.backgroundMs / timeTotal)
        Canvas(Modifier.fillMaxWidth().height(20.dp)) {
            val corner = CornerRadius(size.height / 2f, size.height / 2f)
            val minWidth = size.height
            drawRoundRect(track, size = size, cornerRadius = corner)
            val activeWidth = if (activeFraction > 0f) (size.width * activeFraction).coerceIn(minWidth, size.width) else 0f
            val backgroundWidth = if (backgroundFraction > 0f) (size.width * backgroundFraction).coerceAtLeast(minWidth) else 0f
            if (backgroundWidth > 0f) {
                val start = (activeWidth - minWidth / 2f).coerceAtLeast(0f)
                val width = backgroundWidth.coerceAtMost(size.width - start)
                if (width > 0f) {
                    drawRoundRect(backgroundColor, topLeft = Offset(start, 0f), size = Size(width, size.height), cornerRadius = corner)
                }
            }
            if (activeWidth > 0f) {
                drawRoundRect(activeColor, size = Size(activeWidth, size.height), cornerRadius = corner)
            }
        }
        LegendRow(activeColor, stringResource(R.string.usage_app_active, formatDuration(app.foregroundMs), percentText(activePercent)))
        LegendRow(backgroundColor, stringResource(R.string.usage_app_background, formatDuration(app.backgroundMs), percentText(backgroundPercent)))
    }
}

@Composable
private fun LegendRow(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(16.dp).background(color, CircleShape))
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
