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
    val timestampMs: Long,
) {
    val isCharging: Boolean get() = status == ChargeStatus.CHARGING
}
