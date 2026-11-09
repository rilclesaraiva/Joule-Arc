package com.joulearc.app.data

/** One battery level reading. [plugged] is true while any charger is connected; [screenOn] is the screen state. */
data class LevelSample(
    val timeMs: Long,
    val level: Int,
    val plugged: Boolean,
    val screenOn: Boolean = true,
)
