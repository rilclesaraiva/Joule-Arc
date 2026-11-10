package com.joulearc.app.data

/**
 * One battery level reading. [plugged] is true while any charger is connected; [screenOn] is the screen state.
 * [currentMa] (positive into the battery, negative out of it), [voltageMv] and [tempC] are the readings at
 * that moment; they are null in samples saved by older versions.
 */
data class LevelSample(
    val timeMs: Long,
    val level: Int,
    val plugged: Boolean,
    val screenOn: Boolean = true,
    val currentMa: Int? = null,
    val voltageMv: Int? = null,
    val tempC: Float? = null,
)
