package com.joulearc.app.features.health

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.data.BatteryHealthState
import com.joulearc.app.data.BatterySnapshot

@Composable
fun HealthScreen(
    snapshot: BatterySnapshot?,
    capacityMah: Int?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.health_title),
            style = MaterialTheme.typography.titleLarge,
        )

        val notReported = stringResource(R.string.health_not_reported)
        InfoCard(
            label = stringResource(R.string.health_soh),
            value = snapshot?.healthPercent?.let { stringResource(R.string.health_soh_value, it) } ?: notReported,
            big = snapshot?.healthPercent != null,
        )
        InfoCard(
            label = stringResource(R.string.health_cycles),
            value = snapshot?.cycleCount?.toString() ?: notReported,
            big = snapshot?.cycleCount != null,
        )
        InfoCard(
            label = stringResource(R.string.health_capacity),
            value = capacityMah?.let { stringResource(R.string.health_capacity_value, it) }
                ?: stringResource(R.string.health_capacity_wait),
            big = capacityMah != null,
        )
        InfoCard(
            label = stringResource(R.string.health_state),
            value = stringResource(healthStateLabel(snapshot?.healthState ?: BatteryHealthState.UNKNOWN)),
            big = false,
        )
        snapshot?.technology?.let {
            InfoCard(label = stringResource(R.string.health_technology), value = it, big = false)
        }

        Text(
            text = stringResource(R.string.health_note),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun healthStateLabel(state: BatteryHealthState): Int = when (state) {
    BatteryHealthState.GOOD -> R.string.health_good
    BatteryHealthState.OVERHEAT -> R.string.health_overheat
    BatteryHealthState.DEAD -> R.string.health_dead
    BatteryHealthState.OVER_VOLTAGE -> R.string.health_over_voltage
    BatteryHealthState.COLD -> R.string.health_cold
    BatteryHealthState.FAILURE -> R.string.health_failure
    BatteryHealthState.UNKNOWN -> R.string.health_unknown
}

@Composable
private fun InfoCard(label: String, value: String, big: Boolean) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(
                text = value,
                style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.titleMedium,
            )
        }
    }
}
