package com.joulearc.app.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joulearc.app.R
import com.joulearc.app.core.RangeBar
import com.joulearc.app.core.formatDegrees
import com.joulearc.app.core.formatMinutes
import com.joulearc.app.core.formatPower
import com.joulearc.app.core.formatTemperature
import com.joulearc.app.core.formatVolts
import com.joulearc.app.core.formatWatts
import com.joulearc.app.core.statusPalette
import com.joulearc.app.data.Band
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.PowerSource
import com.joulearc.app.data.PowerState
import com.joulearc.app.data.Scale
import com.joulearc.app.data.Scales
import com.joulearc.app.data.estimateHealthPercent
import java.util.Locale

/** Bottom sheet with the details of one reading: value, rating, range, extra rows and meaning. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingSheet(reading: Reading, state: HomeState, s: BatterySnapshot, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (reading) {
                Reading.CURRENT -> CurrentDetails(state, s)
                Reading.VOLTAGE -> VoltageDetails(state, s)
                Reading.TEMPERATURE -> TemperatureDetails(state, s)
                Reading.LEVEL -> LevelDetails(state, s)
                Reading.POWER -> PowerDetails(state, s)
                Reading.STATUS -> StatusDetails(s)
                Reading.SOURCE -> SourceDetails(state, s)
                Reading.HEALTH -> HealthDetails(state, s)
                Reading.TECHNOLOGY -> SimpleDetails(
                    R.drawable.ic_chip, stringResource(R.string.health_technology),
                    s.technology ?: stringResource(R.string.health_not_reported),
                    stringResource(R.string.meaning_technology),
                )
                Reading.CYCLES -> SimpleDetails(
                    R.drawable.ic_cycle, stringResource(R.string.health_cycles),
                    s.cycleCount?.toString() ?: stringResource(R.string.health_not_reported),
                    stringResource(R.string.meaning_cycles),
                )
            }
        }
    }
}

@Composable
private fun Header(icon: Int, title: String, band: Band?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon, MaterialTheme.colorScheme.primary)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
        )
        band?.let { RatingChip(it) }
    }
}

@Composable
private fun BigValue(text: String) {
    Text(text, style = MaterialTheme.typography.displayMedium)
}

@Composable
private fun ScaleBar(scale: Scale, value: Float?, format: (Float) -> String) {
    RangeBar(
        fraction = value?.let { scale.fraction(it) },
        segments = scale.segments(statusPalette()),
        labels = scale.marks(format),
    )
}

@Composable
private fun Meaning(text: String, band: Band?) {
    MeaningBox(
        title = stringResource(R.string.sheet_meaning),
        text = text,
        color = band?.let { statusPalette().of(it.tone) },
    )
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun chargerRow(s: BatterySnapshot): Pair<String, String>? {
    val v = s.chargerMaxVoltageMv ?: return null
    val a = s.chargerMaxCurrentMa ?: return null
    val text = String.format(
        Locale.getDefault(), "%.0f V · %.2f A · %.0f W", v / 1000f, a / 1000f, (s.chargerMaxMw ?: 0) / 1000f,
    )
    return stringResource(R.string.sheet_charger_max) to text
}

@Composable
private fun CurrentDetails(state: HomeState, s: BatterySnapshot) {
    val band = powerBand(s)
    Header(R.drawable.ic_bolt, stringResource(R.string.sheet_current), band)
    BigValue(s.signedCurrentMa?.let { "$it mA" } ?: "—")
    val r = state.recorded
    val draining = s.drawingFromBattery
    InfoRows(
        listOfNotNull(
            stringResource(R.string.hero_recorded_max) to
                ((if (draining) r.drainMaxMa?.let { -it } else r.chargeMaxMa)?.let { "$it mA" } ?: "—"),
            stringResource(R.string.hero_recorded_min) to
                ((if (draining) r.drainMinMa?.let { -it } else r.chargeMinMa)?.let { "$it mA" } ?: "—"),
            state.estimate?.avgCurrentMa?.let { stringResource(R.string.sheet_average) to "$it mA" },
            chargerRow(s),
        ),
    )
    Meaning(stringResource(R.string.meaning_current), null)
}

@Composable
private fun VoltageDetails(state: HomeState, s: BatterySnapshot) {
    val volts = s.voltageMv / 1000f
    val band = Scales.voltage.rate(volts)
    Header(R.drawable.ic_battery, stringResource(R.string.stat_voltage), band)
    BigValue(formatVolts(s.voltageMv, 3))
    ScaleBar(Scales.voltage, volts, trimUnit("V"))
    InfoRows(
        listOfNotNull(
            state.extremes.minVoltageMv?.let { stringResource(R.string.sheet_min_opened) to formatVolts(it, 3) },
            state.extremes.maxVoltageMv?.let { stringResource(R.string.sheet_max_opened) to formatVolts(it, 3) },
            stringResource(R.string.sheet_typical) to "3.4 – 4.4 V",
        ),
    )
    Meaning(stringResource(meaningOf(band.label, Scales.voltage)), band)
    Note(stringResource(R.string.note_voltage))
}

@Composable
private fun TemperatureDetails(state: HomeState, s: BatterySnapshot) {
    val f = state.fahrenheit
    val band = Scales.temperature.rate(s.temperatureC)
    Header(R.drawable.ic_thermometer, stringResource(R.string.stat_temperature), band)
    BigValue(formatTemperature(s.temperatureC, f))
    ScaleBar(Scales.temperature, s.temperatureC) { formatDegrees(it, f) }
    InfoRows(
        listOfNotNull(
            state.extremes.minTempC?.let { stringResource(R.string.sheet_min_opened) to formatTemperature(it, f) },
            state.extremes.maxTempC?.let { stringResource(R.string.sheet_max_opened) to formatTemperature(it, f) },
        ),
    )
    Meaning(stringResource(meaningOf(band.label, Scales.temperature)), band)
    Note(stringResource(R.string.note_temperature))
}

@Composable
private fun LevelDetails(state: HomeState, s: BatterySnapshot) {
    val band = Scales.level.rate(s.levelPercent.toFloat())
    Header(R.drawable.ic_battery, stringResource(R.string.stat_level), band)
    BigValue("${s.levelPercent}%")
    ScaleBar(Scales.level, s.levelPercent.toFloat()) { "${it.toInt()}%" }
    val e = state.estimate
    InfoRows(
        listOfNotNull(
            s.chargeCounterUah?.let { stringResource(R.string.capacity_now) to "${formatMah(it / 1000f)} mAh" },
            state.capacityMah?.let { stringResource(R.string.capacity_full) to stringResource(R.string.value_mah, it) },
            if (s.isCharging) e?.minutesToFull?.let { stringResource(R.string.hero_time_to_full) to formatMinutes(it) } else null,
            if (s.drawingFromBattery) e?.minutesLeft?.let { stringResource(R.string.hero_remaining) to formatMinutes(it) } else null,
        ),
    )
    Meaning(stringResource(meaningOf(band.label, Scales.level)), band)
}

@Composable
private fun PowerDetails(state: HomeState, s: BatterySnapshot) {
    val scale = powerScale(s)
    val watts = s.powerMw?.let { it / 1000f }
    val band = if (scale != null && watts != null) scale.rate(watts) else null
    Header(R.drawable.ic_plug, stringResource(R.string.stat_power), band)
    BigValue(s.signedPowerMw?.let { formatPower(it) } ?: "—")
    Text(
        text = stringResource(
            when {
                s.isCharging -> R.string.sheet_power_in
                s.drawingFromBattery -> R.string.sheet_power_out
                else -> stateLabel(s.powerState)
            },
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (scale != null) ScaleBar(scale, watts, trimUnit("W"))
    InfoRows(
        listOfNotNull(
            s.signedCurrentMa?.let { stringResource(R.string.sheet_current) to "$it mA" },
            stringResource(R.string.stat_voltage) to formatVolts(s.voltageMv, 3),
            chargerRow(s),
            state.extremes.maxChargeMw?.let { stringResource(R.string.sheet_fastest_opened) to formatWatts(it) },
            state.extremes.maxDrainMw?.let { stringResource(R.string.sheet_drain_opened) to formatWatts(it) },
        ),
    )
    if (band != null && scale != null) Meaning(stringResource(meaningOf(band.label, scale)), band)
    Note(stringResource(R.string.note_power))
}

@Composable
private fun StatusDetails(s: BatterySnapshot) {
    val palette = statusPalette()
    Header(R.drawable.ic_bolt, stringResource(R.string.tile_status), null)
    BigValue(stringResource(stateLabel(s.powerState)))
    InfoRows(listOf(stringResource(R.string.stat_level) to "${s.levelPercent}%"))
    val meaning = when (s.powerState) {
        PowerState.CHARGING -> R.string.meaning_status_charging
        PowerState.DISCHARGING -> R.string.meaning_status_discharging
        PowerState.FULL -> R.string.meaning_status_full
        PowerState.PLUGGED_DRAINING -> R.string.warn_draining_body
        PowerState.PLUGGED_PAUSED -> R.string.warn_paused_body
        PowerState.CHECKING -> R.string.meaning_status_checking
    }
    MeaningBox(stringResource(R.string.sheet_meaning), stringResource(meaning), palette.of(stateTone(s.powerState)))
}

@Composable
private fun SourceDetails(state: HomeState, s: BatterySnapshot) {
    Header(R.drawable.ic_plug, stringResource(R.string.tile_source), null)
    BigValue(stringResource(sourceLabel(s.source)))
    InfoRows(
        listOfNotNull(
            if (s.source != PowerSource.BATTERY) s.signedPowerMw?.let { stringResource(R.string.stat_power) to formatPower(it) } else null,
            chargerRow(s),
            state.extremes.maxChargeMw?.let { stringResource(R.string.sheet_fastest_opened) to formatWatts(it) },
        ),
    )
    val meaning = when (s.source) {
        PowerSource.AC -> R.string.meaning_source_ac
        PowerSource.USB -> R.string.meaning_source_usb
        PowerSource.WIRELESS -> R.string.meaning_source_wireless
        PowerSource.BATTERY -> R.string.meaning_source_battery
    }
    Meaning(stringResource(meaning), null)
}

@Composable
private fun HealthDetails(state: HomeState, s: BatterySnapshot) {
    val health = estimateHealthPercent(state.capacityMah, state.designCapacityMah)
    val band = health?.let { Scales.health.rate(it.toFloat()) }
    Header(R.drawable.ic_heart, stringResource(R.string.tile_health), band)
    BigValue(health?.let { "$it%" } ?: "—")
    if (health != null) ScaleBar(Scales.health, health.toFloat()) { "${it.toInt()}%" }
    InfoRows(
        listOfNotNull(
            stringResource(R.string.health_condition) to stringResource(conditionLabel(s.healthState)),
            state.capacityMah?.let { stringResource(R.string.capacity_full) to stringResource(R.string.value_mah, it) },
            state.designCapacityMah?.let { stringResource(R.string.capacity_design) to stringResource(R.string.value_mah, it) },
            s.cycleCount?.let { stringResource(R.string.health_cycles) to it.toString() },
        ),
    )
    if (band != null) {
        Meaning(stringResource(meaningOf(band.label, Scales.health)), band)
    } else {
        Meaning(stringResource(R.string.meaning_health_unknown), null)
    }
    Note(stringResource(R.string.note_health))
}

@Composable
private fun SimpleDetails(icon: Int, title: String, value: String, meaning: String) {
    Header(icon, title, null)
    BigValue(value)
    Meaning(meaning, null)
}
