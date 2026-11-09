package com.joulearc.app.data

import java.util.Calendar
import kotlin.math.roundToInt

enum class DayPeriod {
    NIGHT, MORNING, AFTERNOON, EVENING;

    companion object {
        fun ofHour(hour: Int): DayPeriod = when {
            hour < 6 -> NIGHT
            hour < 12 -> MORNING
            hour < 18 -> AFTERNOON
            else -> EVENING
        }
    }
}

data class SpeedStat<T>(val key: T, val avgCurrentMa: Int, val sessions: Int)

data class ChargeInsights(
    val bySource: List<SpeedStat<PowerSource>>,
    val byPeriod: List<SpeedStat<DayPeriod>>,
    /** Set only when there are at least two options and the winner has 2+ sessions. */
    val bestSource: PowerSource?,
    val bestPeriod: DayPeriod?,
    /** Slowest option, with the same conditions as the best one (and different from it). */
    val worstSource: PowerSource?,
    val worstPeriod: DayPeriod?,
    val usedSessions: Int,
)

private const val MIN_SESSION_MS = 300_000L
private const val MAX_START_LEVEL = 80

internal fun localHour(ms: Long): Int =
    Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.HOUR_OF_DAY)

/**
 * Compares the average charging current of past sessions by power source and by time of day.
 * Only finished sessions of 5+ minutes that started below 80% count, so the slow top-up phase
 * does not distort the comparison.
 */
fun computeInsights(
    sessions: List<ChargeSession>,
    hourOf: (Long) -> Int = ::localHour,
): ChargeInsights {
    val eligible = sessions.filter {
        !it.ongoing && it.durationMs >= MIN_SESSION_MS && it.avgCurrentMa > 0.0 &&
            it.startLevel < MAX_START_LEVEL
    }
    val bySource = speedStats(eligible) { it.source }
    val byPeriod = speedStats(eligible) { DayPeriod.ofHour(hourOf(it.startMs)) }
    return ChargeInsights(
        bySource = bySource,
        byPeriod = byPeriod,
        bestSource = best(bySource),
        bestPeriod = best(byPeriod),
        worstSource = worst(bySource),
        worstPeriod = worst(byPeriod),
        usedSessions = eligible.size,
    )
}

private fun <T> speedStats(list: List<ChargeSession>, key: (ChargeSession) -> T): List<SpeedStat<T>> =
    list.groupBy(key).map { (k, group) ->
        val totalMs = group.sumOf { it.durationMs }.toDouble()
        val weighted = group.sumOf { it.avgCurrentMa * it.durationMs } / totalMs
        SpeedStat(k, weighted.roundToInt(), group.size)
    }.sortedByDescending { it.avgCurrentMa }

private fun <T> best(stats: List<SpeedStat<T>>): T? =
    if (stats.size >= 2 && stats.first().sessions >= 2) stats.first().key else null

private fun <T> worst(stats: List<SpeedStat<T>>): T? =
    if (stats.size >= 2 && stats.last().sessions >= 2 && stats.last().key != stats.first().key) stats.last().key else null
