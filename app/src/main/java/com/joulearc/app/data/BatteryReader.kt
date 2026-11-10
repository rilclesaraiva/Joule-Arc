package com.joulearc.app.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager

class BatteryReader(context: Context) {
    private val appContext = context.applicationContext
    private val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager

    init {
        SettingsRepository.init(appContext)
    }

    fun read(): BatterySnapshot? {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> ChargeStatus.CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargeStatus.DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> ChargeStatus.FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargeStatus.NOT_CHARGING
            else -> ChargeStatus.UNKNOWN
        }

        val source = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
            BatteryManager.BATTERY_PLUGGED_AC -> PowerSource.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PowerSource.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PowerSource.WIRELESS
            else -> PowerSource.BATTERY
        }

        val healthState = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealthState.GOOD
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealthState.OVERHEAT
            BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealthState.DEAD
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealthState.OVER_VOLTAGE
            BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealthState.COLD
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealthState.FAILURE
            else -> BatteryHealthState.UNKNOWN
        }

        // Dual-cell phones report half the real current and charge; the user can turn on doubling.
        val multiplier = if (SettingsRepository.dualCellBattery.value) 2 else 1
        val rawCurrent = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val baseMa = unitLearner.normalize(rawCurrent)
        val counter = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            .takeIf { it != Long.MIN_VALUE && it > 0L }
            ?.let { it * multiplier }

        val sign = java.lang.Long.signum(rawCurrent)
        if (baseMa != null) {
            signLearner.observe(
                sign,
                knownDischarging = source == PowerSource.BATTERY && status == ChargeStatus.DISCHARGING,
                magnitudeMa = baseMa,
                knownCharging = source != PowerSource.BATTERY && status == ChargeStatus.CHARGING,
            )
        }
        // Once the sign is learned, the current decides: some phones report DISCHARGING (or a stale sign)
        // while a charger is still pushing current in, for example near the end of a charge.
        val pluggedDischarging = source != PowerSource.BATTERY && status != ChargeStatus.FULL && when {
            baseMa != null && signLearner.isDischarge(sign, baseMa) -> true
            baseMa != null && signLearner.isCharge(sign, baseMa) -> false
            else -> status == ChargeStatus.DISCHARGING
        }

        val cycles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1).takeIf { it > 0 }
        } else null

        // Not public constants: present in the sticky battery broadcast on many devices, absent on others.
        val maxChargerUa = intent.getIntExtra("max_charging_current", 0)
        val maxChargerUv = intent.getIntExtra("max_charging_voltage", 0)
        val plugged = source != PowerSource.BATTERY
        // A charger that is still negotiating looks odd for a few seconds; only a lasting oddity counts.
        val odd = plugged && status != ChargeStatus.FULL && (pluggedDischarging || status != ChargeStatus.CHARGING)
        val confirmed = amberGate.confirmed(odd, System.currentTimeMillis())
        val settling = odd && !confirmed

        return BatterySnapshot(
            levelPercent = percent,
            currentMa = baseMa?.let { it * multiplier },
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            temperatureC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            status = status,
            source = source,
            chargeCounterUah = counter,
            timestampMs = System.currentTimeMillis(),
            cycleCount = cycles,
            healthState = healthState,
            technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf { it.isNotBlank() },
            screenOn = powerManager.isInteractive,
            pluggedDischarging = pluggedDischarging,
            chargerMaxVoltageMv = if (plugged && maxChargerUv > 0) maxChargerUv / 1000 else null,
            chargerMaxCurrentMa = if (plugged && maxChargerUa > 0) maxChargerUa / 1000 else null,
            settling = settling,
        )
    }

    companion object {
        /** Shared by every reader in the process, so the service, tile and widget learn together. */
        private val signLearner = CurrentSignLearner()
        /** Read the saved unit only on first use, after the settings were loaded. */
        val unitLearner: CurrentUnitLearner by lazy {
            CurrentUnitLearner(SettingsRepository.currentUnitMicro()) { SettingsRepository.setCurrentUnitMicro(it) }
        }
        private val amberGate = AmberGate()
    }
}
