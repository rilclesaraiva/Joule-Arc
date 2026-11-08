package com.joulearc.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Small user settings kept in the app's private storage. */
object SettingsRepository {
    private const val KEY_DESIGN_CAPACITY = "design_capacity_mah"

    private var prefs: SharedPreferences? = null

    private val _designCapacityMah = MutableStateFlow<Int?>(null)
    /** Rated battery capacity typed by the user, used to estimate battery health. */
    val designCapacityMah: StateFlow<Int?> = _designCapacityMah.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs = p
        _designCapacityMah.value = p.getInt(KEY_DESIGN_CAPACITY, 0).takeIf { it > 0 }
    }

    fun setDesignCapacityMah(value: Int?) {
        val v = value?.takeIf { it in 500..30_000 }
        _designCapacityMah.value = v
        prefs?.edit()?.apply {
            if (v == null) remove(KEY_DESIGN_CAPACITY) else putInt(KEY_DESIGN_CAPACITY, v)
        }?.apply()
    }
}
