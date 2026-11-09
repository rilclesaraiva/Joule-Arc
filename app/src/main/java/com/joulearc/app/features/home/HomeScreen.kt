package com.joulearc.app.features.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.SupportFooter
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.DeviceInfo
import com.joulearc.app.data.PowerState

/** The only main screen: live readings first, then links to the deeper pages. */
@Composable
fun HomeScreen(
    state: HomeState,
    onRequestNotifications: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDrain: () -> Unit,
    onOpenUsage: () -> Unit,
    onResetRecorded: () -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    var openReading by rememberSaveable { mutableStateOf<Reading?>(null) }
    val s = state.snapshot

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.notificationsGranted) NotificationCard(onRequestNotifications)

        if (s == null) {
            Text(stringResource(R.string.monitor_starting), style = MaterialTheme.typography.titleMedium)
        } else {
            HeroCard(
                s = s,
                estimate = state.estimate,
                recorded = state.recorded,
                onReset = onResetRecorded,
                onClick = { openReading = Reading.CURRENT },
            )
            AnimatedVisibility(visible = s.powerState.isWarning) { WarningCard(s) }
            CapacityCard(
                s = s,
                capacityMah = state.capacityMah,
                designMah = state.designCapacityMah,
                designFromSystem = state.designFromSystem,
                onClick = { openReading = Reading.HEALTH },
            )
            SectionHeader(stringResource(R.string.home_battery), stringResource(R.string.home_tap_hint))
            ReadingGrid(state, s) { openReading = it }
        }

        SectionHeader(stringResource(R.string.home_section_analysis))
        NavCard(
            icon = R.drawable.ic_history,
            title = stringResource(R.string.nav_history_title),
            subtitle = historySubtitle(state),
            onClick = onOpenHistory,
        )
        NavCard(
            icon = R.drawable.ic_trending_down,
            title = stringResource(R.string.drain_title),
            subtitle = drainSubtitle(state),
            onClick = onOpenDrain,
        )
        NavCard(
            icon = R.drawable.ic_apps,
            title = stringResource(R.string.usage_per_app),
            subtitle = stringResource(R.string.nav_usage_sub),
            onClick = onOpenUsage,
        )

        SectionHeader(stringResource(R.string.home_section_device))
        InfoRows(
            listOf(
                stringResource(R.string.device_android) to DeviceInfo.androidVersion,
                stringResource(R.string.device_manufacturer) to DeviceInfo.manufacturer,
                stringResource(R.string.device_model) to DeviceInfo.model,
            ),
        )

        SupportFooter()
    }

    val reading = openReading
    if (reading != null && s != null) {
        ReadingSheet(reading, state, s) { openReading = null }
    }
}

@Composable
private fun historySubtitle(state: HomeState): String {
    val parts = mutableListOf(stringResource(R.string.nav_history_sessions, state.sessionCount))
    state.insights.bestSource?.let {
        parts += stringResource(R.string.insights_best_source, stringResource(sourceLabel(it)))
    }
    return parts.joinToString(" · ")
}

@Composable
private fun drainSubtitle(state: HomeState): String {
    val on = state.drain.screenOnPph
    val off = state.drain.screenOffPph
    return if (on == null && off == null) {
        stringResource(R.string.nav_drain_collecting)
    } else {
        stringResource(
            R.string.nav_drain_rates,
            on?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "—",
            off?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "—",
        )
    }
}

@Composable
private fun NotificationCard(onRequest: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.notifications_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.notifications_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onRequest) { Text(stringResource(R.string.notifications_action)) }
        }
    }
}

/** Amber warning when a charger is connected but the battery is not charging or is losing charge. */
@Composable
private fun WarningCard(s: BatterySnapshot) {
    val (title, body) = when (s.powerState) {
        PowerState.PLUGGED_DRAINING -> R.string.warn_draining_title to R.string.warn_draining_body
        PowerState.PLUGGED_PAUSED -> R.string.warn_paused_title to R.string.warn_paused_body
        else -> return
    }
    val amber = statusPalette().caution
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = amber.copy(alpha = 0.16f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = amber, modifier = Modifier.size(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun NavCard(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon, MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val PowerState.isWarning: Boolean
    get() = this == PowerState.PLUGGED_DRAINING || this == PowerState.PLUGGED_PAUSED
