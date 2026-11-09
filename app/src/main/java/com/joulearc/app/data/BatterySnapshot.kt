package com.joulearc.app.data

enum class ChargeStatus { CHARGING, DISCHARGING, FULL, NOT_CHARGING, UNKNOWN }

enum class PowerSource { AC, USB, WIRELESS, BATTERY }

enum class BatteryHealthState { GOOD, OVERHEAT, DEAD, OVER_VOLTAGE, COLD, FAILURE, UNKNOWN }

/** What is really happening, combining the plug state, the system status and the current direction. */
enum class PowerState {
    CHARGING,
    DISCHARGING,
    FULL,

    /** A charger was just connected with an odd reading: waiting a moment before calling it a problem. */
    CHECKING,

    /** A charger is connected but current flows out of the battery (weak or faulty charger or cable). */
    PLUGGED_DRAINING,

    /** A charger is connected but the battery is not charging (charge limit, protection, or a problem). */
    PLUGGED_PAUSED,
}

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
    /** Charge cycle count, only on Android 14+ and when the manufacturer reports it. */
    val cycleCount: Int? = null,
    val healthState: BatteryHealthState = BatteryHealthState.UNKNOWN,
    val technology: String? = null,
    /** True while the screen is on (interactive). */
    val screenOn: Boolean = true,
    /** True when a charger is connected but current flows out of the battery. */
    val pluggedDischarging: Boolean = false,
    /** What the charger offers, when the system reports it (not on every device). */
    val chargerMaxVoltageMv: Int? = null,
    val chargerMaxCurrentMa: Int? = null,
    /** True while an odd plugged-in reading is still inside the settling period (see [AmberGate]). */
    val settling: Boolean = false,
) {
    val powerState: PowerState
        get() = when {
            source == PowerSource.BATTERY -> PowerState.DISCHARGING
            status == ChargeStatus.FULL -> PowerState.FULL
            settling -> PowerState.CHECKING
            pluggedDischarging -> PowerState.PLUGGED_DRAINING
            status == ChargeStatus.CHARGING -> PowerState.CHARGING
            else -> PowerState.PLUGGED_PAUSED
        }

    /** True only while the battery is really receiving charge. */
    val isCharging: Boolean get() = powerState == PowerState.CHARGING

    /** True while current flows out of the battery, with or without a charger. */
    val drawingFromBattery: Boolean
        get() = powerState == PowerState.DISCHARGING || powerState == PowerState.PLUGGED_DRAINING ||
            (powerState == PowerState.CHECKING && pluggedDischarging)

    /** Current in mA: positive into the battery, negative out of it. */
    val signedCurrentMa: Int?
        get() = currentMa?.let { if (drawingFromBattery) -it else it }

    /** Power in mW (voltage x current), always positive. Null without a current reading. */
    val powerMw: Int? get() = currentMa?.let { (voltageMv.toLong() * it / 1000L).toInt() }

    /** Power in mW: positive into the battery, negative out of it. */
    val signedPowerMw: Int?
        get() = powerMw?.let { if (drawingFromBattery) -it else it }

    /** Maximum power the charger offers, in mW, when the system reports it. */
    val chargerMaxMw: Int?
        get() = if (chargerMaxVoltageMv != null && chargerMaxCurrentMa != null) {
            (chargerMaxVoltageMv.toLong() * chargerMaxCurrentMa / 1000L).toInt()
        } else null
}
