package com.joulearc.app.data

import kotlin.math.abs

/** Readings at or above this magnitude can only be microamps (no phone draws 20 A). */
private const val MICROAMP_THRESHOLD = 20_000L

/**
 * `BATTERY_PROPERTY_CURRENT_NOW` is documented in µA, but many devices report mA and the sign
 * convention differs between vendors. [microamps] says which unit the device uses; when it is not
 * known yet, a reading with magnitude >= 20 000 is taken as µA and a smaller one as mA.
 * Returns the absolute current in mA, or null when the device reports nothing usable.
 */
fun normalizeCurrentMa(raw: Long, microamps: Boolean? = null): Int? {
    if (raw == Long.MIN_VALUE || raw == 0L) return null
    val magnitude = abs(raw)
    val micro = microamps ?: (magnitude >= MICROAMP_THRESHOLD)
    val ma = if (micro) maxOf(1L, (magnitude + 500L) / 1000L) else magnitude
    return ma.toInt()
}

/**
 * Learns the unit of `CURRENT_NOW` per device. A phone that reports µA also reports small values in
 * µA (a 15 mA standby reading arrives as 15 000), which the plain size rule would show as 15 A. Once
 * a few readings of 20 000 or more were seen, the device is known to use µA and every reading is
 * divided by 1000. A single glitch does not decide it.
 */
class CurrentUnitLearner(initial: Boolean? = null, private val onLearned: (Boolean) -> Unit = {}) {
    @Volatile var microamps: Boolean? = initial
        private set
    @Volatile private var bigReadings = 0

    fun normalize(raw: Long): Int? {
        if (raw != Long.MIN_VALUE && microamps == null && abs(raw) >= MICROAMP_THRESHOLD) {
            bigReadings++
            if (bigReadings >= MIN_BIG_READINGS) {
                microamps = true
                onLearned(true)
            }
        }
        return normalizeCurrentMa(raw, microamps)
    }

    /** For the export: "uA" once learned, otherwise "unknown" (the size rule is used). */
    val label: String get() = if (microamps == true) "uA" else "unknown"

    private companion object {
        const val MIN_BIG_READINGS = 3
    }
}
