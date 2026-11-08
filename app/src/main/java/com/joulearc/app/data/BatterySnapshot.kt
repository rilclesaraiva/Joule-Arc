package com.joulearc.app.data

enum class ChargeStatus { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

enum class PowerSource { AC, USB, WIRELESS, BATTERY }

data class BatterySnapshot(
    val levelPercent: Int,
    /** Absolute current in mA, or null when the device does not report it. */
    val currentMa: Int?,
    val voltageMv: Int,
    val temperatureC: Float,
    val status: ChargeStatus,
    val source: PowerSource,
    /** Charge currently stored in the battery in µAh, or null when not reported. */
    val chargeCounterUah: Long?,
    val timestampMs: Long,
) {
    val isCharging: Boolean get() = status == ChargeStatus.CHARGING

    /** Power in mW (voltage x current), positive. Null without a current reading. */
    val powerMw: Int? get() = currentMa?.let { (voltageMv.toLong() * it / 1000L).toInt() }

    /** Positive while a charger is connected, negative while running on the battery. */
    val signedPowerMw: Int?
        get() = powerMw?.let { if (source == PowerSource.BATTERY) -it else it }
}
