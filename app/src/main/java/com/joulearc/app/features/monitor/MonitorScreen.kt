package com.joulearc.app.features.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.formatClock
import com.joulearc.app.core.formatMinutes
import com.joulearc.app.core.formatPower
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeEstimate
import com.joulearc.app.data.PowerSource
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MonitorScreen(
    snapshot: BatterySnapshot?,
    estimate: ChargeEstimate?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (snapshot == null) {
            Text(
                text = stringResource(R.string.monitor_starting),
                style = MaterialTheme.typography.titleMedium,
            )
            return@Column
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(MonitorService.statusLabel(snapshot.status)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        if (snapshot.source != PowerSource.BATTERY) R.string.current_label_charging
                        else R.string.current_label_discharging,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                )
                val signedCurrent = snapshot.signedCurrentMa
                if (signedCurrent != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = signedCurrent.toString(),
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            text = stringResource(R.string.unit_ma),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 28.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.current_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }

        if (snapshot.isCharging) {
            EstimateCard(snapshot, estimate)
        }

        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${stringResource(R.string.stat_level)}: ${snapshot.levelPercent}%",
                    style = MaterialTheme.typography.titleMedium,
                )
                LinearWavyProgressIndicator(
                    progress = { snapshot.levelPercent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                label = stringResource(R.string.stat_voltage),
                value = String.format(Locale.getDefault(), "%.2f V", snapshot.voltageMv / 1000f),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_temperature),
                value = String.format(Locale.getDefault(), "%.1f °C", snapshot.temperatureC),
                modifier = Modifier.weight(1f),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                label = stringResource(R.string.stat_power),
                value = snapshot.signedPowerMw?.let { formatPower(it) } ?: "—",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = stringResource(R.string.stat_source),
                value = stringResource(
                    when (snapshot.source) {
                        PowerSource.AC -> R.string.source_ac
                        PowerSource.USB -> R.string.source_usb
                        PowerSource.WIRELESS -> R.string.source_wireless
                        PowerSource.BATTERY -> R.string.source_battery
                    },
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EstimateCard(snapshot: BatterySnapshot, estimate: ChargeEstimate?) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.estimate_title),
                style = MaterialTheme.typography.titleMedium,
            )
            val perHour = estimate?.percentPerHour
            if (perHour != null) {
                Text(
                    text = stringResource(R.string.estimate_rate, perHour),
                    style = MaterialTheme.typography.headlineMedium,
                )
                val minutes = estimate?.minutesToFull
                if (minutes != null) {
                    val eta = snapshot.timestampMs + minutes * 60_000L
                    Text(
                        text = stringResource(R.string.estimate_full, formatMinutes(minutes), formatClock(eta)),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                estimate?.capacityMah?.let {
                    Text(
                        text = stringResource(R.string.estimate_capacity, it),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.estimate_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = stringResource(
                        if (snapshot.chargeCounterUah == null) R.string.estimate_unavailable
                        else R.string.estimate_calculating,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
