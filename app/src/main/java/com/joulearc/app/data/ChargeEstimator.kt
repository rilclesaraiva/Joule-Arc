package com.joulearc.app.data

import kotlin.math.roundToInt

data class ChargeEstimate(
    /** % of the battery per hour at the current speed (gained while charging, lost while draining). */
    val percentPerHour: Float?,
    val minutesToFull: Int?,
    /** Full battery capacity estimated from the charge counter and the level. */
    val capacityMah: Int?,
    /** Average current over the recent window. */
    val avgCurrentMa: Int?,
    /** Minutes until empty at the current drain, while the battery is draining. */
    val minutesLeft: Int? = null,
)

/**
 * Estimates speed and time from the recent average current and the battery capacity
 * (charge counter / level). It does not model the slowdown near 100%, so the real time
 * to full is usually longer than the estimate.
 */
class ChargeEstimator(private val windowSize: Int = 30) {
    private val currents = ArrayDeque<Int>()
    private val drains = ArrayDeque<Int>()
    private var capacity: Float? = null
    private var lastLevel = -1

    /** Latest capacity estimate in mAh, kept across charging and discharging. */
    val capacityMah: Int? get() = capacity?.roundToInt()

    fun update(s: BatterySnapshot): ChargeEstimate {
        val counter = s.chargeCounterUah
        // The counter moves continuously but the level only in whole percents, so sampling on every
        // reading makes the estimate saw-tooth. Sample only when the level changes, and smooth slowly.
        if (counter != null && s.levelPercent >= 10 && (capacity == null || s.levelPercent != lastLevel)) {
            val sample = counter / 1000f / (s.levelPercent / 100f)
            capacity = capacity?.let { it * 0.9f + sample * 0.1f } ?: sample
        }
        lastLevel = s.levelPercent
        val cap = capacity

        if (s.isCharging) {
            drains.clear()
            push(currents, s.currentMa)
            val avg = currents.averageOrNull()
            val perHour = if (avg != null && cap != null && cap > 0f) avg / cap * 100f else null
            val minutes = if (perHour != null && perHour > 0.1f) {
                ((100 - s.levelPercent) / perHour * 60f).roundToInt()
            } else null
            return ChargeEstimate(perHour, minutes, cap?.roundToInt(), avg?.roundToInt())
        }

        currents.clear()
        if (!s.drawingFromBattery) {
            drains.clear()
            return ChargeEstimate(null, null, cap?.roundToInt(), null)
        }
        push(drains, s.currentMa)
        val avg = drains.averageOrNull()
        val perHour = if (avg != null && cap != null && cap > 0f) avg / cap * 100f else null
        val remainingMah = counter?.let { it / 1000f } ?: cap?.let { it * s.levelPercent / 100f }
        val minutesLeft = if (avg != null && avg >= 1f && remainingMah != null) {
            (remainingMah / avg * 60f).roundToInt()
        } else null
        return ChargeEstimate(perHour, null, cap?.roundToInt(), avg?.roundToInt(), minutesLeft)
    }

    private fun push(window: ArrayDeque<Int>, value: Int?) {
        value ?: return
        window.addLast(value)
        if (window.size > windowSize) window.removeFirst()
    }

    private fun ArrayDeque<Int>.averageOrNull(): Float? = if (isEmpty()) null else average().toFloat()
}
