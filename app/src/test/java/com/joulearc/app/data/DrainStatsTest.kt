package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DrainStatsTest {
    private val hour = 3_600_000L
    private fun s(t: Long, level: Int, screenOn: Boolean = true, plugged: Boolean = false) =
        LevelSample(t, level, plugged, screenOn)
    private val hourOf = { ms: Long -> (ms / hour).toInt() % 24 }

    @Test fun separatesScreenOnAndOff() {
        val samples = listOf(
            s(0, 100, screenOn = true), s(hour, 90, screenOn = true),          // 10 %/h on
            s(2 * hour, 88, screenOn = false), s(3 * hour, 86, screenOn = false), // 2 %/h off (a.screenOn false)
        )
        val r = computeDrainStats(samples, 0, 10 * hour, hourOf)
        // intervals: (0->1h on, drop 10), (1h->2h on, drop 2), (2h->3h off, drop 2) => on: 12 in 2h, off: 2 in 1h
        assertEquals(6f, r.screenOnPph!!, 0.01f)
        assertEquals(2f, r.screenOffPph!!, 0.01f)
        assertEquals(2f, r.screenOnHours, 0.01f)
    }

    @Test fun ignoresChargingAndLongGaps() {
        val samples = listOf(
            s(0, 50), s(hour, 40, plugged = true),   // involves charging: skipped
            s(2 * hour, 60, plugged = true), s(3 * hour, 70),
            s(10 * hour, 20),                         // 7h gap: skipped
        )
        val r = computeDrainStats(samples, 0, 20 * hour, hourOf)
        assertNull(r.screenOnPph)
        assertEquals(0f, r.screenOnHours, 0.001f)
    }

    @Test fun needsHalfHourPerState() {
        val r = computeDrainStats(listOf(s(0, 100), s(10 * 60_000L, 99)), 0, hour, hourOf)
        assertNull(r.screenOnPph)
    }

    @Test fun findsHighestAndLowestPeriod() {
        // hours 1..2 (night): 10 %/h ; hours 13..14 (afternoon): 2 %/h
        val samples = listOf(
            s(1 * hour, 100), s(2 * hour, 90), s(3 * hour, 80),
            s(13 * hour, 80), s(14 * hour, 78), s(15 * hour, 76),
        )
        val r = computeDrainStats(samples, 0, 24 * hour, hourOf)
        assertEquals(DayPeriod.NIGHT, r.highestPeriod)
        assertEquals(DayPeriod.AFTERNOON, r.lowestPeriod)
        assertEquals(10f, r.byPeriod.first().percentPerHour, 0.01f)
    }

    @Test fun noPeriodWinnerWithSinglePeriod() {
        val r = computeDrainStats(listOf(s(1 * hour, 100), s(2 * hour, 90), s(3 * hour, 80)), 0, 24 * hour, hourOf)
        assertNull(r.highestPeriod)
    }
}
