package com.joulearc.app.data

import kotlin.math.abs

/**
 * `BATTERY_PROPERTY_CURRENT_NOW` is documented in µA, but many devices report mA and the sign
 * convention differs between vendors. Values with magnitude >= 20 000 are treated as µA.
 * Returns the absolute current in mA, or null when the device reports nothing usable.
 */
fun normalizeCurrentMa(raw: Long): Int? {
    if (raw == Long.MIN_VALUE || raw == 0L) return null
    val magnitude = abs(raw)
    val ma = if (magnitude >= 20_000L) magnitude / 1000L else magnitude
    return ma.toInt()
}
