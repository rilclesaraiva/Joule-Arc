package com.joulearc.app.data

/** One battery level reading. [plugged] is true while any charger is connected. */
data class LevelSample(val timeMs: Long, val level: Int, val plugged: Boolean)
