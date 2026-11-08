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
import com.joulearc.app.data.ChargeSession
import com.joulearc.app.data.PowerSource

@Composable
fun HistoryScreen(
    active: ChargeSession?,
    sessions: List<ChargeSession>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
