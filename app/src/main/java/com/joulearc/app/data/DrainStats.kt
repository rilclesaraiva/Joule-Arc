package com.joulearc.app.data

private const val HOUR_MS = 3_600_000f
private const val MAX_GAP_MS = 2L * 3_600_000L
private const val MIN_STATE_HOURS = 0.5f
private const val MIN_PERIOD_HOURS = 1f

data class RateStat<T>(val key: T, val percentPerHour: Float, val hours: Float)

/** Battery consumption while unplugged, from the level history. */
data class DrainStats(
    /** Percent of battery per hour with the screen on, or null without enough data. */
    val screenOnPph: Float?,
    /** Percent of battery per hour with the screen off (standby), or null without enough data. */
    val screenOffPph: Float?,
    val screenOnHours: Float,
    val screenOffHours: Float,
    /** Consumption by time of day (screen on and off together), highest first. */
    val byPeriod: List<RateStat<DayPeriod>>,
    /** Set only with at least two periods that have 1+ hour of data. */
    val highestPeriod: DayPeriod?,
    val lowestPeriod: DayPeriod?,
)

/**
 * Consumption rates from consecutive unplugged level samples between [fromMs] and [toMs].
 * Each interval takes the screen state and the period of its first sample, and intervals longer
 * than 2 hours are skipped (the monitor was probably not running).
 */
fun computeDrainStats(
    samples: List<LevelSample>,
    fromMs: Long,
    toMs: Long,
    hourOf: (Long) -> Int = ::localHour,
): DrainStats {
    var onDrop = 0f; var onMs = 0L
    var offDrop = 0f; var offMs = 0L
    val periodDrop = HashMap<DayPeriod, Float>()
    val periodMs = HashMap<DayPeriod, Long>()

    samples.zipWithNext().forEach { (a, b) ->
        val dt = b.timeMs - a.timeMs
        if (a.plugged || b.plugged || b.timeMs !in fromMs..toMs || dt <= 0L || dt > MAX_GAP_MS) return@forEach
        val drop = (a.level - b.level).coerceAtLeast(0).toFloat()
        if (a.screenOn) { onDrop += drop; onMs += dt } else { offDrop += drop; offMs += dt }
        val period = DayPeriod.ofHour(hourOf(a.timeMs))
        periodDrop.merge(period, drop, Float::plus)
        periodMs.merge(period, dt, Long::plus)
    }

    val onHours = onMs / HOUR_MS
    val offHours = offMs / HOUR_MS
    val periods = periodDrop.keys
        .map { RateStat(it, periodDrop.getValue(it) / (periodMs.getValue(it) / HOUR_MS), periodMs.getValue(it) / HOUR_MS) }
        .filter { it.hours >= MIN_PERIOD_HOURS }
        .sortedByDescending { it.percentPerHour }
    val comparable = periods.size >= 2

    return DrainStats(
        screenOnPph = if (onHours >= MIN_STATE_HOURS) onDrop / onHours else null,
        screenOffPph = if (offHours >= MIN_STATE_HOURS) offDrop / offHours else null,
        screenOnHours = onHours,
        screenOffHours = offHours,
        byPeriod = periods,
        highestPeriod = if (comparable) periods.first().key else null,
        lowestPeriod = if (comparable) periods.last().key else null,
    )
}
