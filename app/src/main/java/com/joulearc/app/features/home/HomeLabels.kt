package com.joulearc.app.features.home

import androidx.compose.ui.graphics.Color
import com.joulearc.app.R
import com.joulearc.app.core.StatusPalette
import com.joulearc.app.core.Tone
import com.joulearc.app.core.formatTrim
import com.joulearc.app.data.Band
import com.joulearc.app.data.BatteryHealthState
import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.DayPeriod
import com.joulearc.app.data.PowerSource
import com.joulearc.app.data.PowerState
import com.joulearc.app.data.RatingLabel
import com.joulearc.app.data.Scale
import com.joulearc.app.data.Scales

internal fun stateLabel(state: PowerState): Int = when (state) {
    PowerState.CHARGING -> R.string.status_charging
    PowerState.DISCHARGING -> R.string.status_discharging
    PowerState.FULL -> R.string.status_full
    PowerState.PLUGGED_DRAINING -> R.string.status_draining
    PowerState.PLUGGED_PAUSED -> R.string.status_paused
    PowerState.CHECKING -> R.string.status_checking
}

internal fun stateTone(state: PowerState): Tone = when (state) {
    PowerState.CHARGING, PowerState.FULL -> Tone.GOOD
    PowerState.DISCHARGING -> Tone.BAD
    PowerState.PLUGGED_DRAINING, PowerState.PLUGGED_PAUSED -> Tone.CAUTION
    PowerState.CHECKING -> Tone.INFO
}

internal fun sourceLabel(source: PowerSource): Int = when (source) {
    PowerSource.AC -> R.string.source_ac
    PowerSource.USB -> R.string.source_usb
    PowerSource.WIRELESS -> R.string.source_wireless
    PowerSource.BATTERY -> R.string.source_on_battery
}

internal fun periodLabel(period: DayPeriod): Int = when (period) {
    DayPeriod.NIGHT -> R.string.period_night
    DayPeriod.MORNING -> R.string.period_morning
    DayPeriod.AFTERNOON -> R.string.period_afternoon
    DayPeriod.EVENING -> R.string.period_evening
}

internal fun conditionLabel(state: BatteryHealthState): Int = when (state) {
    BatteryHealthState.GOOD -> R.string.health_good
    BatteryHealthState.OVERHEAT -> R.string.health_overheat
    BatteryHealthState.DEAD -> R.string.health_dead
    BatteryHealthState.OVER_VOLTAGE -> R.string.health_over_voltage
    BatteryHealthState.COLD -> R.string.health_cold
    BatteryHealthState.FAILURE -> R.string.health_failure
    BatteryHealthState.UNKNOWN -> R.string.health_unknown
}

internal fun ratingLabel(label: RatingLabel): Int = when (label) {
    RatingLabel.TOO_LOW -> R.string.rating_too_low
    RatingLabel.LOW -> R.string.rating_low
    RatingLabel.NORMAL -> R.string.rating_normal
    RatingLabel.HIGH -> R.string.rating_high
    RatingLabel.TOO_HIGH -> R.string.rating_too_high
    RatingLabel.TOO_COLD -> R.string.rating_too_cold
    RatingLabel.COLD -> R.string.rating_cold
    RatingLabel.WARM -> R.string.rating_warm
    RatingLabel.HOT -> R.string.rating_hot
    RatingLabel.TOO_HOT -> R.string.rating_too_hot
    RatingLabel.CRITICAL -> R.string.rating_critical
    RatingLabel.GOOD -> R.string.rating_good
    RatingLabel.FULLISH -> R.string.rating_fullish
    RatingLabel.SLOW -> R.string.rating_slow
    RatingLabel.FAST -> R.string.rating_fast
    RatingLabel.VERY_FAST -> R.string.rating_very_fast
    RatingLabel.ULTRA_FAST -> R.string.rating_ultra_fast
    RatingLabel.LIGHT_USE -> R.string.rating_light_use
    RatingLabel.NORMAL_USE -> R.string.rating_normal_use
    RatingLabel.HEAVY_USE -> R.string.rating_heavy_use
    RatingLabel.VERY_HEAVY_USE -> R.string.rating_very_heavy_use
    RatingLabel.FAIR -> R.string.rating_fair
    RatingLabel.WORN -> R.string.rating_worn
    RatingLabel.POOR -> R.string.rating_poor
}

/** What a rating means, in plain words. */
/** [plugged] is true while a charger is connected: some advice only makes sense then. */
internal fun meaningOf(label: RatingLabel, scale: Scale, plugged: Boolean = true): Int = when {
    scale === Scales.voltage -> when (label) {
        RatingLabel.LOW -> R.string.meaning_voltage_low
        RatingLabel.HIGH -> if (plugged) R.string.meaning_voltage_high else R.string.meaning_voltage_high_unplugged
        RatingLabel.TOO_HIGH -> if (plugged) R.string.meaning_voltage_too_high else R.string.meaning_voltage_too_high_unplugged
        else -> R.string.meaning_voltage_normal
    }
    scale === Scales.temperature -> when (label) {
        RatingLabel.TOO_COLD -> R.string.meaning_temp_too_cold
        RatingLabel.COLD -> R.string.meaning_temp_cold
        RatingLabel.WARM -> R.string.meaning_temp_warm
        RatingLabel.HOT -> if (plugged) R.string.meaning_temp_hot else R.string.meaning_temp_hot_unplugged
        RatingLabel.TOO_HOT -> if (plugged) R.string.meaning_temp_too_hot else R.string.meaning_temp_too_hot_unplugged
        else -> R.string.meaning_temp_normal
    }
    scale === Scales.level -> when (label) {
        RatingLabel.CRITICAL -> if (plugged) R.string.meaning_level_critical_charging else R.string.meaning_level_critical
        RatingLabel.LOW -> if (plugged) R.string.meaning_level_low_charging else R.string.meaning_level_low
        RatingLabel.FULLISH -> if (plugged) R.string.meaning_level_high_charging else R.string.meaning_level_high
        else -> R.string.meaning_level_good
    }
    scale === Scales.chargingPower -> when (label) {
        RatingLabel.SLOW -> R.string.meaning_power_slow
        RatingLabel.NORMAL -> R.string.meaning_power_normal
        else -> R.string.meaning_power_fast
    }
    scale === Scales.dischargePower -> when (label) {
        RatingLabel.LIGHT_USE -> R.string.meaning_use_light
        RatingLabel.NORMAL_USE -> R.string.meaning_use_normal
        else -> R.string.meaning_use_heavy
    }
    else -> when (label) {
        RatingLabel.POOR -> R.string.meaning_health_poor
        RatingLabel.WORN -> R.string.meaning_health_worn
        RatingLabel.FAIR -> R.string.meaning_health_fair
        else -> R.string.meaning_health_good
    }
}

/** Power scale and rating for the current direction, or null when nothing is flowing. */
internal fun powerScale(s: BatterySnapshot): Scale? = when {
    s.drawingFromBattery -> Scales.dischargePower
    s.isCharging -> Scales.chargingPower
    else -> null
}

internal fun powerBand(s: BatterySnapshot): Band? {
    val mw = s.powerMw ?: return null
    return powerScale(s)?.rate(mw / 1000f)
}

/** Equal-width colored segments for a scale. */
internal fun Scale.segments(palette: StatusPalette): List<Pair<Float, Color>> =
    bands.mapIndexed { i, band -> (i + 1f) / bands.size to palette.of(band.tone) }

/** Labels at the band boundaries, formatted by [format]. */
internal fun Scale.marks(format: (Float) -> String): List<Pair<Float, String>> =
    bands.dropLast(1).mapIndexed { i, band -> (i + 1f) / bands.size to format(band.mark) }

internal fun trimUnit(unit: String): (Float) -> String = { "${formatTrim(it)} $unit" }
