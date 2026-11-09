package com.joulearc.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User settings and recorded values, kept in the app's private storage. */
object SettingsRepository {
    const val DEFAULT_CHARGE_ALERT_LEVEL = 80
    const val DEFAULT_LOW_ALERT_LEVEL = 15

    /** 0 = follow the system, 1 = light, 2 = dark. */
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val KEY_DESIGN_CAPACITY = "design_capacity_mah"
    private const val KEY_CHARGE_ALERT = "charge_alert"
    private const val KEY_CHARGE_ALERT_LEVEL = "charge_alert_level"
    private const val KEY_LOW_ALERT = "low_alert"
    private const val KEY_LOW_ALERT_LEVEL = "low_alert_level"
    private const val KEY_DUAL_CELL = "dual_cell"
    private const val KEY_THEME = "theme"
    private const val KEY_FAHRENHEIT = "fahrenheit"
    private const val KEY_REC_CHARGE_MAX = "rec_charge_max"
    private const val KEY_REC_CHARGE_MIN = "rec_charge_min"
    private const val KEY_REC_DRAIN_MAX = "rec_drain_max"
    private const val KEY_REC_DRAIN_MIN = "rec_drain_min"

    private var prefs: SharedPreferences? = null

    private val _designCapacityMah = MutableStateFlow<Int?>(null)
    /** Design capacity typed by the user; null means "use the system value". */
    val designCapacityMah: StateFlow<Int?> = _designCapacityMah.asStateFlow()

    private val _chargeAlertEnabled = MutableStateFlow(false)
    val chargeAlertEnabled: StateFlow<Boolean> = _chargeAlertEnabled.asStateFlow()
    private val _chargeAlertLevel = MutableStateFlow(DEFAULT_CHARGE_ALERT_LEVEL)
    val chargeAlertLevel: StateFlow<Int> = _chargeAlertLevel.asStateFlow()

    private val _lowAlertEnabled = MutableStateFlow(false)
    val lowAlertEnabled: StateFlow<Boolean> = _lowAlertEnabled.asStateFlow()
    private val _lowAlertLevel = MutableStateFlow(DEFAULT_LOW_ALERT_LEVEL)
    val lowAlertLevel: StateFlow<Int> = _lowAlertLevel.asStateFlow()

    private val _dualCellBattery = MutableStateFlow(false)
    val dualCellBattery: StateFlow<Boolean> = _dualCellBattery.asStateFlow()

    private val _themeMode = MutableStateFlow(THEME_SYSTEM)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _useFahrenheit = MutableStateFlow(false)
    val useFahrenheit: StateFlow<Boolean> = _useFahrenheit.asStateFlow()

    private val _recorded = MutableStateFlow(RecordedCurrents())
    val recorded: StateFlow<RecordedCurrents> = _recorded.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        _designCapacityMah.value = p.getInt(KEY_DESIGN_CAPACITY, 0).takeIf { it > 0 }
        _chargeAlertEnabled.value = p.getBoolean(KEY_CHARGE_ALERT, false)
        _chargeAlertLevel.value = p.getInt(KEY_CHARGE_ALERT_LEVEL, DEFAULT_CHARGE_ALERT_LEVEL)
        _lowAlertEnabled.value = p.getBoolean(KEY_LOW_ALERT, false)
        _lowAlertLevel.value = p.getInt(KEY_LOW_ALERT_LEVEL, DEFAULT_LOW_ALERT_LEVEL)
        _dualCellBattery.value = p.getBoolean(KEY_DUAL_CELL, false)
        _themeMode.value = p.getInt(KEY_THEME, THEME_SYSTEM)
        _useFahrenheit.value = p.getBoolean(KEY_FAHRENHEIT, false)
        _recorded.value = RecordedCurrents(
            chargeMaxMa = p.getInt(KEY_REC_CHARGE_MAX, -1).takeIf { it >= 0 },
            chargeMinMa = p.getInt(KEY_REC_CHARGE_MIN, -1).takeIf { it >= 0 },
            drainMaxMa = p.getInt(KEY_REC_DRAIN_MAX, -1).takeIf { it >= 0 },
            drainMinMa = p.getInt(KEY_REC_DRAIN_MIN, -1).takeIf { it >= 0 },
        )
        prefs = p
    }

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        prefs?.edit()?.run {
            block()
            apply()
        }
    }

    fun setDesignCapacityMah(value: Int?) {
        val v = value?.takeIf { it in 500..30_000 }
        _designCapacityMah.value = v
        edit { if (v == null) remove(KEY_DESIGN_CAPACITY) else putInt(KEY_DESIGN_CAPACITY, v) }
    }

    fun setChargeAlertEnabled(value: Boolean) {
        _chargeAlertEnabled.value = value
        edit { putBoolean(KEY_CHARGE_ALERT, value) }
    }

    fun setChargeAlertLevel(value: Int) {
        val v = value.coerceIn(50, 100)
        _chargeAlertLevel.value = v
        edit { putInt(KEY_CHARGE_ALERT_LEVEL, v) }
    }

    fun setLowAlertEnabled(value: Boolean) {
        _lowAlertEnabled.value = value
        edit { putBoolean(KEY_LOW_ALERT, value) }
    }

    fun setLowAlertLevel(value: Int) {
        val v = value.coerceIn(5, 50)
        _lowAlertLevel.value = v
        edit { putInt(KEY_LOW_ALERT_LEVEL, v) }
    }

    fun setDualCellBattery(value: Boolean) {
        _dualCellBattery.value = value
        edit { putBoolean(KEY_DUAL_CELL, value) }
    }

    fun setThemeMode(value: Int) {
        _themeMode.value = value
        edit { putInt(KEY_THEME, value) }
    }

    fun setUseFahrenheit(value: Boolean) {
        _useFahrenheit.value = value
        edit { putBoolean(KEY_FAHRENHEIT, value) }
    }

    /** Updates the recorded highest and lowest current; writes only when a value changes. */
    fun recordCurrent(snapshot: BatterySnapshot) {
        val ma = snapshot.signedCurrentMa ?: return
        val current = _recorded.value
        val updated = current.update(ma, snapshot.isCharging, snapshot.drawingFromBattery)
        if (updated != current) {
            _recorded.value = updated
            saveRecorded(updated)
        }
    }

    fun resetRecorded() {
        _recorded.value = RecordedCurrents()
        saveRecorded(RecordedCurrents())
    }

    private fun saveRecorded(r: RecordedCurrents) {
        edit {
            putInt(KEY_REC_CHARGE_MAX, r.chargeMaxMa ?: -1)
            putInt(KEY_REC_CHARGE_MIN, r.chargeMinMa ?: -1)
            putInt(KEY_REC_DRAIN_MAX, r.drainMaxMa ?: -1)
            putInt(KEY_REC_DRAIN_MIN, r.drainMinMa ?: -1)
        }
    }
}
