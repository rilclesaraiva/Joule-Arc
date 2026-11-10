package com.joulearc.app.features.home

import androidx.compose.foundation.layout.Arrangement
import com.joulearc.app.core.formatInt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.MiniGauge
import com.joulearc.app.core.formatPower
import com.joulearc.app.core.formatTemperature
import com.joulearc.app.core.formatVolts
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.Band
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.Scale
import com.joulearc.app.data.Scales
import com.joulearc.app.data.estimateHealthPercent

private data class Tile(
    val reading: Reading,
    val title: Int,
    val value: String,
    val band: Band? = null,
    val scale: Scale? = null,
    val scaleValue: Float? = null,
    val icon: Int? = null,
)

/** Three-column grid of readings; each tile opens its detail sheet. */
@Composable
internal fun ReadingGrid(state: HomeState, s: BatterySnapshot, onOpen: (Reading) -> Unit) {
    val volts = s.voltageMv / 1000f
    val watts = s.powerMw?.let { it / 1000f }
    val health = estimateHealthPercent(state.capacityMah, state.designCapacityMah)
    val pScale = powerScale(s)
    val notReported = "—"
    val tiles = listOf(
        Tile(Reading.VOLTAGE, R.string.stat_voltage, formatVolts(s.voltageMv), Scales.voltage.rate(volts), Scales.voltage, volts),
        Tile(
            Reading.TEMPERATURE, R.string.stat_temperature, formatTemperature(s.temperatureC, state.fahrenheit),
            Scales.temperature.rate(s.temperatureC), Scales.temperature, s.temperatureC,
        ),
        Tile(
            Reading.LEVEL, R.string.stat_level, "${s.levelPercent}%",
            Scales.level.rate(s.levelPercent.toFloat()), Scales.level, s.levelPercent.toFloat(),
        ),
        Tile(
            Reading.POWER, R.string.stat_power, s.signedPowerMw?.let { formatPower(it) } ?: notReported,
            band = if (pScale != null && watts != null) pScale.rate(watts) else null,
            scale = pScale, scaleValue = watts, icon = R.drawable.ic_plug,
        ),
        Tile(Reading.STATUS, R.string.tile_status, stringResource(stateLabel(s.powerState)), icon = R.drawable.ic_bolt),
        Tile(Reading.SOURCE, R.string.tile_source, stringResource(sourceLabel(s.source)), icon = R.drawable.ic_plug),
        Tile(
            Reading.HEALTH, R.string.tile_health, health?.let { "$it%" } ?: notReported,
            band = health?.let { Scales.health.rate(it.toFloat()) }, icon = R.drawable.ic_heart,
        ),
        Tile(Reading.TECHNOLOGY, R.string.health_technology, s.technology ?: notReported, icon = R.drawable.ic_chip),
        Tile(Reading.CYCLES, R.string.health_cycles, s.cycleCount?.let { formatInt(it) } ?: notReported, icon = R.drawable.ic_cycle),
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        tiles.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { TileCard(it, onOpen, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TileCard(tile: Tile, onOpen: (Reading) -> Unit, modifier: Modifier) {
    val palette = statusPalette()
    ElevatedCard(onClick = { onOpen(tile.reading) }, modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (tile.scale != null && tile.scaleValue != null) {
                MiniGauge(tile.scale.fraction(tile.scaleValue), tile.scale.segments(palette))
            } else if (tile.icon != null) {
                IconBadge(tile.icon, MaterialTheme.colorScheme.primary)
            }
            Text(
                text = stringResource(tile.title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(tile.value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            tile.band?.let {
                Text(
                    text = stringResource(ratingLabel(it.label)),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.of(it.tone),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
