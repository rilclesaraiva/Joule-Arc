package com.joulearc.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.BidirectionalGauge
import com.joulearc.app.core.formatClock
import com.joulearc.app.core.formatMinutes
import com.joulearc.app.core.formatWatts
import com.joulearc.app.core.gaugeRange
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeEstimate
import com.joulearc.app.data.RecordedCurrents
import kotlin.math.abs

/** Main card: current gauge, speed and time estimates, recorded highest and lowest current. */
@Composable
internal fun HeroCard(
    s: BatterySnapshot,
    estimate: ChargeEstimate?,
    recorded: RecordedCurrents,
    onReset: () -> Unit,
    onClick: () -> Unit,
) {
    val palette = statusPalette()
    val signed = s.signedCurrentMa
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${s.levelPercent}%", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = stringResource(R.string.home_battery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            BidirectionalGauge(
                value = (signed ?: 0).toFloat(),
                range = gaugeRange(abs(signed ?: 0)),
                negativeColor = palette.bad,
                positiveColor = palette.good,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.status_discharging),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s.drawingFromBattery) palette.bad else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val band = powerBand(s)
                if (band != null) {
                    RatingChip(band)
                } else {
                    ToneChip(stringResource(stateLabel(s.powerState)), palette.of(stateTone(s.powerState)))
                }
                Text(
                    text = stringResource(R.string.status_charging),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (s.isCharging) palette.good else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(signed?.toString() ?: "—", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Medium)
                Text(
                    text = " " + stringResource(R.string.unit_ma),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            SpeedRow(s, estimate)

            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            RecordedRow(s, recorded, onReset)
        }
    }
}

@Composable
private fun SpeedRow(s: BatterySnapshot, estimate: ChargeEstimate?) {
    val measuring = stringResource(R.string.hero_measuring)
    val watts = s.powerMw?.let { formatWatts(it) }
    val stateText = stringResource(stateLabel(s.powerState))
    val leftSub = listOfNotNull(stateText, watts).joinToString(" · ")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        when {
            s.isCharging -> {
                InfoItem(
                    icon = R.drawable.ic_speed,
                    title = estimate?.percentPerHour?.let { stringResource(R.string.hero_per_hour, it) } ?: measuring,
                    subtitle = leftSub,
                    modifier = Modifier.weight(1f),
                )
                val minutes = estimate?.minutesToFull
                InfoItem(
                    icon = R.drawable.ic_clock,
                    title = minutes?.let { formatMinutes(it) } ?: measuring,
                    subtitle = minutes?.let {
                        stringResource(R.string.hero_full_at, formatClock(s.timestampMs + it * 60_000L))
                    } ?: stringResource(R.string.hero_time_to_full),
                    modifier = Modifier.weight(1f),
                )
            }
            s.drawingFromBattery -> {
                InfoItem(
                    icon = R.drawable.ic_speed,
                    title = estimate?.percentPerHour?.let { stringResource(R.string.hero_per_hour_drain, it) } ?: measuring,
                    subtitle = leftSub,
                    modifier = Modifier.weight(1f),
                )
                InfoItem(
                    icon = R.drawable.ic_clock,
                    title = estimate?.minutesLeft?.let { formatMinutes(it) } ?: measuring,
                    subtitle = stringResource(R.string.hero_remaining),
                    modifier = Modifier.weight(1f),
                )
            }
            else -> InfoItem(
                icon = R.drawable.ic_bolt,
                title = stateText,
                subtitle = watts ?: "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InfoItem(icon: Int, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecordedRow(s: BatterySnapshot, recorded: RecordedCurrents, onReset: () -> Unit) {
    val draining = s.drawingFromBattery
    val max = if (draining) recorded.drainMaxMa?.let { -it } else recorded.chargeMaxMa
    val min = if (draining) recorded.drainMinMa?.let { -it } else recorded.chargeMinMa
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.hero_recorded_max) + "  " + (max?.let { "$it mA" } ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.hero_recorded_min) + "  " + (min?.let { "$it mA" } ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onReset) { Text(stringResource(R.string.hero_reset)) }
    }
}
