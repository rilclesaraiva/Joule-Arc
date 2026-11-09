package com.joulearc.app.data

import kotlin.math.max

/** One point of a session curve: seconds since the start, battery level and charging current. */
data class SessionPoint(val offsetSec: Int, val level: Int, val currentMa: Int)

data class ChargeSession(
    val startMs: Long,
    /** Timestamp of the last charging sample. */
    val lastMs: Long,
    val startLevel: Int,
    val endLevel: Int,
    val peakCurrentMa: Int,
    /** Time-weighted average charging current. */
    val avgCurrentMa: Double,
    val peakPowerMw: Int,
    val maxTempC: Float,
    val source: PowerSource,
    val ongoing: Boolean,
    /** Sparse curve (every 30 s or when the level changes), oldest first. */
    val points: List<SessionPoint> = emptyList(),
) {
    val durationMs: Long get() = lastMs - startMs
    val gainPercent: Int get() = endLevel - startLevel

    /** Average % per hour, only for sessions long enough to be meaningful. */
    val percentPerHour: Float?
        get() = if (durationMs >= 300_000L && gainPercent > 0) {
            gainPercent * 3_600_000f / durationMs
        } else null
}

private const val POINT_INTERVAL_MS = 30_000L
private const val MAX_POINTS = 360

/**
 * Groups charging samples into sessions. A session starts with the first charging sample and
 * ends when the charger is unplugged or no charging sample arrives for [gapMs].
 */
class ChargeSessionTracker(
    initial: ChargeSession? = null,
    private val gapMs: Long = 120_000L,
) {
    var current: ChargeSession? = initial
        private set

    /** Returns the session that just ended, if any. */
    fun onSnapshot(s: BatterySnapshot): ChargeSession? {
        val cur = current
        if (s.isCharging) {
            if (cur != null && s.timestampMs - cur.lastMs <= gapMs) {
                current = cur.plus(s)
                return null
            }
            current = start(s)
            return cur?.copy(ongoing = false)
        }
        if (cur != null && (s.source == PowerSource.BATTERY || s.timestampMs - cur.lastMs > gapMs)) {
            current = null
            return cur.copy(ongoing = false)
        }
        return null
    }

    private fun start(s: BatterySnapshot) = ChargeSession(
        startMs = s.timestampMs,
        lastMs = s.timestampMs,
        startLevel = s.levelPercent,
        endLevel = s.levelPercent,
        peakCurrentMa = s.currentMa ?: 0,
        avgCurrentMa = s.currentMa?.toDouble() ?: 0.0,
        peakPowerMw = s.powerMw ?: 0,
        maxTempC = s.temperatureC,
        source = s.source,
        ongoing = true,
        points = listOf(SessionPoint(0, s.levelPercent, s.currentMa ?: 0)),
    )

    private fun ChargeSession.plus(s: BatterySnapshot): ChargeSession {
        val c = s.currentMa
        val elapsed = (lastMs - startMs).toDouble()
        val dt = (s.timestampMs - lastMs).toDouble()
        val avg = if (c != null && elapsed + dt > 0.0) (avgCurrentMa * elapsed + c * dt) / (elapsed + dt)
        else avgCurrentMa

        val offsetSec = ((s.timestampMs - startMs) / 1000L).toInt()
        val lastPoint = points.lastOrNull()
        val newPoints = if (
            lastPoint == null || lastPoint.level != s.levelPercent ||
            (offsetSec - lastPoint.offsetSec) * 1000L >= POINT_INTERVAL_MS
        ) {
            val appended = points + SessionPoint(offsetSec, s.levelPercent, c ?: 0)
            if (appended.size > MAX_POINTS) {
                appended.filterIndexed { i, _ -> i % 2 == 0 || i == appended.lastIndex }
            } else appended
        } else points

        return copy(
            lastMs = s.timestampMs,
            endLevel = s.levelPercent,
            peakCurrentMa = max(peakCurrentMa, c ?: 0),
            avgCurrentMa = avg,
            peakPowerMw = max(peakPowerMw, s.powerMw ?: 0),
            maxTempC = max(maxTempC, s.temperatureC),
            points = newPoints,
        )
    }
}
