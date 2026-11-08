package com.joulearc.app.data

enum class ChargeStatus { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

enum class PowerSource { AC, USB, WIRELESS, BATTERY }

enum class BatteryHealthState { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, FAILURE, UNKNOWN }

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
    /** State of health in %, only on Android 14+ and when the manufacturer reports it. */
    val healthPercent: Int? = null,
    /** Charge cycle count, only on Android 14+ and when the manufacturer reports it. */
    val cycleCount: Int? = null,
    val healthState: BatteryHealthState = BatteryHealthState.UNKNOWN,
    val technology: String? = null,
) {
    val isCharging: Boolean get() = status == ChargeStatus.CHARGING

    /** Current in mA: positive with a charger connected, negative while running on the battery. */
    val signedCurrentMa: Int?
        get() = currentMa?.let { if (source == PowerSource.BATTERY) -it else it }

    /** Power in mW (voltage x current), always positive. Null without a current reading. */
    val powerMw: Int? get() = currentMa?.let { (voltageMv.toLong() * it / 1000L).toInt() }

    /** Positive while a charger is connected, negative while running on the battery. */
    val signedPowerMw: Int?
        get() = powerMw?.let { if (source == PowerSource.BATTERY) -it else it }
}
