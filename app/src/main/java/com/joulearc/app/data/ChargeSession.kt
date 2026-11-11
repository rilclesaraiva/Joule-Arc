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
    /** Times the charger was unplugged and plugged back within [RESUME_WINDOW_MS]; the session carried on. */
    val pauses: List<SessionPause> = emptyList(),
    /** True while the charger is unplugged and the session waits for a reconnection. */
    val paused: Boolean = false,
) {
    val durationMs: Long get() = lastMs - startMs
    val pausedMs: Long get() = pauses.sumOf { it.durationSec * 1000L }
    /** Time actually charging: the duration without the pauses. */
    val chargingMs: Long get() = durationMs - pausedMs
    val gainPercent: Int get() = endLevel - startLevel

    /** Average % per hour, only for sessions long enough to be meaningful. */
    val percentPerHour: Float?
        get() = if (chargingMs >= 300_000L && gainPercent > 0) {
            gainPercent * 3_600_000f / chargingMs
        } else null
}

/** Charging stopped at [offsetSec] after the start of the session and came back [durationSec] later. */
data class SessionPause(val offsetSec: Int, val durationSec: Int)

/** Unplugged for no longer than this, the next charge continues the same session. */
const val RESUME_WINDOW_MS = 5 * 60_000L

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

    /** A session whose charger was unplugged less than [RESUME_WINDOW_MS] ago and may still resume. */
    var pending: ChargeSession? = null
        private set

    /** Returns the session that just ended, if any. */
    fun onSnapshot(s: BatterySnapshot): ChargeSession? {
        val cur = current
        val waiting = pending
        if (s.isCharging) {
            if (cur != null && s.timestampMs - cur.lastMs <= gapMs) {
                current = cur.plus(s)
                return null
            }
            if (cur == null && waiting != null && s.timestampMs - waiting.lastMs <= RESUME_WINDOW_MS) {
                current = waiting.resume(s)
                pending = null
                return null
            }
            current = start(s)
            pending = null
            return (cur ?: waiting)?.copy(ongoing = false, paused = false)
        }
        if (cur != null) {
            // A full battery has nothing left to resume: unplugging at 100% ends the session at once.
            if (s.source == PowerSource.BATTERY && cur.endLevel < 100 && s.timestampMs - cur.lastMs <= RESUME_WINDOW_MS) {
                // Unplugged: wait a few minutes in case it was only a reconnection.
                current = null
                pending = cur.copy(paused = true)
                return null
            }
            if (s.source == PowerSource.BATTERY || s.timestampMs - cur.lastMs > gapMs) {
                current = null
                return cur.copy(ongoing = false)
            }
        }
        if (waiting != null && s.timestampMs - waiting.lastMs > RESUME_WINDOW_MS) {
            pending = null
            return waiting.copy(ongoing = false, paused = false)
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

    /** Continues the session after a short unplug: the gap is recorded as a pause and not averaged in. */
    private fun ChargeSession.resume(s: BatterySnapshot): ChargeSession {
        val c = s.currentMa ?: 0
        val pause = SessionPause(((lastMs - startMs) / 1000L).toInt(), ((s.timestampMs - lastMs) / 1000L).toInt())
        val offsetSec = ((s.timestampMs - startMs) / 1000L).toInt()
        return copy(
            lastMs = s.timestampMs,
            endLevel = s.levelPercent,
            peakCurrentMa = max(peakCurrentMa, c),
            peakPowerMw = max(peakPowerMw, s.powerMw ?: 0),
            maxTempC = max(maxTempC, s.temperatureC),
            points = points + SessionPoint(offsetSec, s.levelPercent, c),
            pauses = pauses + pause,
            ongoing = true,
            paused = false,
        )
    }

    private fun ChargeSession.plus(s: BatterySnapshot): ChargeSession {
        val c = s.currentMa
        val elapsed = (lastMs - startMs - pausedMs).toDouble()
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
