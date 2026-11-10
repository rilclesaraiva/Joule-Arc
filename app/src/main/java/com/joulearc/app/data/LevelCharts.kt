package com.joulearc.app.data

import java.util.Calendar
import java.util.TimeZone

private const val HOUR_MS = 3_600_000L
private const val DAY_MS = 24 * HOUR_MS

/** Battery level at the end of one hour, and whether a charger was connected during it. */
data class HourBar(val startMs: Long, val level: Int?, val plugged: Boolean)

/** Battery percentage consumed during one local day. Null when there is no data for the day. */
data class DayBar(val startMs: Long, val drainPercent: Float?)

fun truncateToHour(ms: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = ms
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun startOfDay(ms: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = ms
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** The last [hours] hourly buckets, oldest first. The last one is the current (partial) hour. */
fun hourlyLevels(
    samples: List<LevelSample>,
    nowMs: Long,
    hours: Int = 24,
    zone: TimeZone = TimeZone.getDefault(),
): List<HourBar> {
    val lastStart = truncateToHour(nowMs, zone)
    return (hours - 1 downTo 0).map { k ->
        val start = lastStart - k * HOUR_MS
        val end = start + HOUR_MS
        val level = samples.lastOrNull { it.timeMs < end }?.level
        val plugged = samples.any { it.timeMs in start until end && it.plugged }
        HourBar(start, level, plugged)
    }
}

/** The 24 hourly buckets of the local day starting at [dayStartMs]; hours still in the future have no level. */
fun dayHourlyLevels(samples: List<LevelSample>, dayStartMs: Long, nowMs: Long): List<HourBar> =
    (0 until 24).map { k ->
        val start = dayStartMs + k * HOUR_MS
        val end = start + HOUR_MS
        if (start > nowMs) {
            HourBar(start, null, false)
        } else {
            HourBar(start, samples.lastOrNull { it.timeMs < end }?.level, samples.any { it.timeMs in start until end && it.plugged })
        }
    }

/** Percentage consumed while unplugged, per local day, for the last [days] days (oldest first). */
fun dailyDrain(
    samples: List<LevelSample>,
    nowMs: Long,
    days: Int = 14,
    zone: TimeZone = TimeZone.getDefault(),
): List<DayBar> {
    val today = startOfDay(nowMs, zone)
    val starts = (days - 1 downTo 0).map { k -> startOfDay(today - k * DAY_MS + DAY_MS / 2, zone) }
    val daysWithData = samples.map { startOfDay(it.timeMs, zone) }.toHashSet()
    val drops = HashMap<Long, Float>()
    samples.zipWithNext().forEach { (a, b) ->
        if (!a.plugged && !b.plugged && b.level < a.level) {
            drops.merge(startOfDay(b.timeMs, zone), (a.level - b.level).toFloat(), Float::plus)
        }
    }
    return starts.map { DayBar(it, if (it in daysWithData) drops[it] ?: 0f else null) }
}

/** Total percentage consumed while unplugged between [fromMs] and [toMs]. */
fun drainedPercent(samples: List<LevelSample>, fromMs: Long, toMs: Long): Float =
    samples.zipWithNext().sumOf { (a, b) ->
        if (b.timeMs in fromMs..toMs && !a.plugged && !b.plugged && b.level < a.level) {
            (a.level - b.level).toDouble()
        } else 0.0
    }.toFloat()

/** When the battery last reached 100% (start of the last run at 100%), or null. */
fun lastFullCharge(samples: List<LevelSample>): Long? {
    var i = samples.indexOfLast { it.level >= 100 }
    if (i < 0) return null
    while (i > 0 && samples[i - 1].level >= 100) i--
    return samples[i].timeMs
}

/** The service samples at least every 10 minutes, so a longer silence means it was not running. */
const val SAMPLE_GAP_MS = 30 * 60_000L

/** Stretches without level samples longer than [SAMPLE_GAP_MS], as (from, to) pairs. */
fun sampleGaps(samples: List<LevelSample>, minMs: Long = SAMPLE_GAP_MS): List<Pair<Long, Long>> =
    samples.zipWithNext().filter { (a, b) -> b.timeMs - a.timeMs > minMs }.map { (a, b) -> a.timeMs to b.timeMs }
