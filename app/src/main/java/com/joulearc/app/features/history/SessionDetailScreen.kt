package com.joulearc.app.features.history

import androidx.compose.foundation.layout.Arrangement
import com.joulearc.app.core.formatInt
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.LineChart
import com.joulearc.app.core.formatClock
import com.joulearc.app.core.formatDateTime
import com.joulearc.app.core.formatDuration
import com.joulearc.app.core.formatWatts
import com.joulearc.app.core.statusPalette
import com.joulearc.app.core.PageTitle
import com.joulearc.app.data.ChargeSession

/** One charging session with its current and level curves. */
@Composable
fun SessionDetailScreen(session: ChargeSession, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageTitle(stringResource(R.string.page_session))
        Text(formatDateTime(session.startMs), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.history_level, session.startLevel, session.endLevel, session.gainPercent),
            style = MaterialTheme.typography.headlineMedium,
        )
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.history_duration, formatDuration(session.durationMs)))
                Text(stringResource(R.string.history_avg, formatInt(session.avgCurrentMa.toInt())))
                Text(stringResource(R.string.history_peak, formatInt(session.peakCurrentMa), formatWatts(session.peakPowerMw)))
                session.percentPerHour?.let { Text(stringResource(R.string.history_rate, it)) }
                Text(stringResource(R.string.history_temp, session.maxTempC))
            }
        }
        if (session.pauses.isNotEmpty()) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.session_pauses, session.pauses.size, formatDuration(session.pausedMs)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    session.pauses.forEach {
                        Text(
                            stringResource(
                                R.string.session_pause_detail,
                                formatClock(session.startMs + it.offsetSec * 1000L),
                                formatDuration(it.durationSec * 1000L),
                            ),
                        )
                    }
                    Text(
                        stringResource(R.string.session_pauses_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        val points = session.points
        if (points.size < 2) {
            Text(stringResource(R.string.session_no_chart), style = MaterialTheme.typography.bodyMedium)
            return@Column
        }
        val totalSec = points.last().offsetSec.coerceAtLeast(1).toFloat()
        val maxCurrent = points.maxOf { it.currentMa }.coerceAtLeast(1).toFloat()
        val xLabels = listOf(formatClock(session.startMs), formatClock(session.startMs + points.last().offsetSec * 1000L))
        val palette = statusPalette()

        Text(stringResource(R.string.session_chart_current), style = MaterialTheme.typography.titleSmall)
        LineChart(
            points = points.map { Offset(it.offsetSec / totalSec, it.currentMa / maxCurrent) },
            color = palette.good,
            yLabels = listOf("${maxCurrent.toInt()}", "${(maxCurrent / 2).toInt()}", "0"),
            xLabels = xLabels,
        )
        Text(stringResource(R.string.session_chart_level), style = MaterialTheme.typography.titleSmall)
        LineChart(
            points = points.map { Offset(it.offsetSec / totalSec, it.level / 100f) },
            color = MaterialTheme.colorScheme.primary,
            yLabels = listOf("100%", "50%", "0%"),
            xLabels = xLabels,
        )
        Text(
            text = stringResource(R.string.session_chart_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
