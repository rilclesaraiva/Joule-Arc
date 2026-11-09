package com.joulearc.app.features.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.formatDateTime
import com.joulearc.app.core.formatDuration
import com.joulearc.app.core.formatWatts
import com.joulearc.app.data.ChargeInsights
import com.joulearc.app.data.ChargeSession
import com.joulearc.app.data.DayPeriod
import com.joulearc.app.data.PowerSource

@Composable
fun HistoryScreen(
    active: ChargeSession?,
    sessions: List<ChargeSession>,
    insights: ChargeInsights,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "insights") { InsightsCard(insights) }
        if (active != null) {
            item(key = "active") { SessionCard(active) }
        }
        if (sessions.isEmpty() && active == null) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        items(sessions, key = { it.startMs }) { SessionCard(it) }
    }
}

@Composable
private fun SessionCard(session: ChargeSession) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (session.ongoing) stringResource(R.string.history_active)
                else formatDateTime(session.startMs),
                style = MaterialTheme.typography.titleMedium,
                color = if (session.ongoing) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            if (session.ongoing) {
                Text(
                    text = formatDateTime(session.startMs),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(
                    R.string.history_level,
                    session.startLevel,
                    session.endLevel,
                    session.gainPercent,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.history_duration, formatDuration(session.durationMs)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.history_avg, session.avgCurrentMa.toInt()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.history_peak,
                    session.peakCurrentMa,
                    formatWatts(session.peakPowerMw),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            session.percentPerHour?.let {
                Text(
                    text = stringResource(R.string.history_rate, it),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.history_temp, session.maxTempC),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    when (session.source) {
                        PowerSource.AC -> R.string.source_ac
                        PowerSource.USB -> R.string.source_usb
                        PowerSource.WIRELESS -> R.string.source_wireless
                        PowerSource.BATTERY -> R.string.source_battery
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun sourceRes(source: PowerSource): Int = when (source) {
    PowerSource.AC -> R.string.source_ac
    PowerSource.USB -> R.string.source_usb
    PowerSource.WIRELESS -> R.string.source_wireless
    PowerSource.BATTERY -> R.string.source_battery
}

private fun periodRes(period: DayPeriod): Int = when (period) {
    DayPeriod.NIGHT -> R.string.period_night
    DayPeriod.MORNING -> R.string.period_morning
    DayPeriod.AFTERNOON -> R.string.period_afternoon
    DayPeriod.EVENING -> R.string.period_evening
}

@Composable
private fun InsightsCard(insights: ChargeInsights) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.insights_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (insights.usedSessions == 0) {
                Text(
                    text = stringResource(R.string.insights_need_more),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }
            insights.bestSource?.let {
                Text(
                    text = stringResource(R.string.insights_best_source, stringResource(sourceRes(it))),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            insights.bestPeriod?.let {
                Text(
                    text = stringResource(R.string.insights_best_period, stringResource(periodRes(it))),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            insights.worstSource?.let {
                Text(
                    text = stringResource(R.string.insights_worst_source, stringResource(sourceRes(it))),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            insights.worstPeriod?.let {
                Text(
                    text = stringResource(R.string.insights_worst_period, stringResource(periodRes(it))),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (insights.bestSource == null && insights.bestPeriod == null) {
                Text(
                    text = stringResource(R.string.insights_need_more),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.insights_by_source),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            insights.bySource.forEach {
                Text(
                    text = stringResource(
                        R.string.insights_row,
                        stringResource(sourceRes(it.key)),
                        it.avgCurrentMa,
                        it.sessions,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.insights_by_period),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            insights.byPeriod.forEach {
                Text(
                    text = stringResource(
                        R.string.insights_row,
                        stringResource(periodRes(it.key)),
                        it.avgCurrentMa,
                        it.sessions,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = stringResource(R.string.insights_note),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
