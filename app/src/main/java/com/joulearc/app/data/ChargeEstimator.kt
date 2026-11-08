package com.joulearc.app.data

import kotlin.math.roundToInt

data class ChargeEstimate(
    /** Estimated % gained per hour at the current charging speed. */
    val percentPerHour: Float?,
    val minutesToFull: Int?,
    /** Full battery capacity estimated from the charge counter and the level. */
    val capacityMah: Int?,
    /** Average charging current over the recent window. */
    val avgCurrentMa: Int?,
)

/**
 * Estimates charging speed from the recent average current and the battery capacity
 * (charge counter / level). It does not model the slowdown near 100%, so the real time
 * to full is usually longer than the estimate.
 */
class ChargeEstimator(private val windowSize: Int = 30) {
    private val currents = ArrayDeque<Int>()
    private var capacityMah: Float? = null

    fun update(s: BatterySnapshot): ChargeEstimate? {
        if (!s.isCharging) {
            currents.clear()
            return null
        }
        s.currentMa?.let {
            currents.addLast(it)
            if (currents.size > windowSize) currents.removeFirst()
        }
        val counter = s.chargeCounterUah
        if (counter != null && s.levelPercent >= 10) {
            val sample = counter / 1000f / (s.levelPercent / 100f)
            capacityMah = capacityMah?.let { it * 0.9f + sample * 0.1f } ?: sample
        }

        val avg = if (currents.isEmpty()) null else currents.average().toFloat()
        val capacity = capacityMah
        val perHour = if (avg != null && capacity != null && capacity > 0f) avg / capacity * 100f else null
        val minutes = if (perHour != null && perHour > 0.1f) {
            ((100 - s.levelPercent) / perHour * 60f).roundToInt()
        } else null
        return ChargeEstimate(perHour, minutes, capacity?.roundToInt(), avg?.roundToInt())
    }
}
