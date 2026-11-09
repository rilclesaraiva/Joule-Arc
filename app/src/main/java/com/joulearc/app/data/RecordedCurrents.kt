package com.joulearc.app.data

import kotlin.math.max
import kotlin.math.min

/** Highest and lowest current ever recorded (until reset), kept apart for charging and draining. */
data class RecordedCurrents(
    val chargeMaxMa: Int? = null,
    val chargeMinMa: Int? = null,
    /** Draining values are stored as positive magnitudes. */
    val drainMaxMa: Int? = null,
    val drainMinMa: Int? = null,
) {
    fun update(signedMa: Int, charging: Boolean, draining: Boolean): RecordedCurrents {
        val magnitude = kotlin.math.abs(signedMa)
        if (magnitude < MIN_MA) return this
        return when {
            charging && signedMa > 0 -> copy(
                chargeMaxMa = max(chargeMaxMa ?: magnitude, magnitude),
                chargeMinMa = min(chargeMinMa ?: magnitude, magnitude),
            )
            draining && signedMa < 0 -> copy(
                drainMaxMa = max(drainMaxMa ?: magnitude, magnitude),
                drainMinMa = min(drainMinMa ?: magnitude, magnitude),
            )
            else -> this
        }
    }

    private companion object {
        const val MIN_MA = 10
    }
}
