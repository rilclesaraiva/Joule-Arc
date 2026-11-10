package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class LevelChartsTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val t0 = 1_700_000_000_000L - (1_700_000_000_000L % day) // a UTC midnight

    private fun s(t: Long, level: Int, plugged: Boolean = false) = LevelSample(t, level, plugged)

    @Test fun truncatesToHourAndDay() {
        assertEquals(t0 + 5 * hour, truncateToHour(t0 + 5 * hour + 123_456, utc))
        assertEquals(t0, startOfDay(t0 + 17 * hour, utc))
    }

    @Test fun hourlyCarriesLevelForwardAndMarksPlugged() {
        val now = t0 + 10 * hour + 30 * 60_000
        val samples = listOf(s(t0 + 7 * hour + 600_000, 80), s(t0 + 9 * hour + 60_000, 60, plugged = true))
        val bars = hourlyLevels(samples, now, hours = 4, zone = utc)
        assertEquals(4, bars.size)
        assertEquals(t0 + 7 * hour, bars[0].startMs)
        assertEquals(80, bars[0].level)
        assertEquals(80, bars[1].level)       // 8:00 carries 80
        assertEquals(60, bars[2].level)
        assertTrue(bars[2].plugged)
        assertEquals(60, bars[3].level)       // current hour, carried
        assertFalse(bars[3].plugged)
    }

    @Test fun hourlyIsNullBeforeFirstSample() {
        val now = t0 + 3 * hour
        val bars = hourlyLevels(listOf(s(t0 + 3 * hour, 50)), now, hours = 3, zone = utc)
        assertNull(bars[0].level)
        assertNull(bars[1].level)
        assertEquals(50, bars[2].level)
    }

    @Test fun dailyDrainSumsUnpluggedDropsOnly() {
        val now = t0 + day + 12 * hour
        val samples = listOf(
            s(t0 + 1 * hour, 100), s(t0 + 5 * hour, 90), s(t0 + 9 * hour, 70),   // day 0 drops 10 + 20
            s(t0 + 10 * hour, 30, plugged = true), s(t0 + 11 * hour, 60, plugged = true), // charging, ignored
            s(t0 + day + 2 * hour, 55), s(t0 + day + 6 * hour, 50),             // day 1 drops 5
        )
        val bars = dailyDrain(samples, now, days = 3, zone = utc)
        assertEquals(3, bars.size)
        assertNull(bars[0].drainPercent)          // day before first sample
        assertEquals(30f, bars[1].drainPercent!!, 0.01f)
        assertEquals(5f, bars[2].drainPercent!!, 0.01f)
    }

    @Test fun drainedPercentRespectsWindow() {
        val samples = listOf(s(0, 100), s(hour, 90), s(2 * hour, 70), s(3 * hour, 60))
        assertEquals(40f, drainedPercent(samples, 0, 3 * hour), 0.01f)
        assertEquals(30f, drainedPercent(samples, hour + 1, 3 * hour), 0.01f)
    }

    @Test fun lastFullChargeIsStartOfLastRunAt100() {
        val samples = listOf(
            s(1, 100, true), s(2, 90),
            s(10, 99, true), s(11, 100, true), s(12, 100, true), s(13, 95),
        )
        assertEquals(11L, lastFullCharge(samples))
        assertNull(lastFullCharge(listOf(s(1, 50))))
    }

    @Test fun dayHourlyHasNoLevelForFutureHours() {
        val now = t0 + 10 * hour + 30 * 60_000
        val samples = listOf(s(t0 + 2 * hour, 90), s(t0 + 5 * hour, 70, plugged = true))
        val bars = dayHourlyLevels(samples, t0, now)
        assertEquals(24, bars.size)
        assertEquals(t0, bars[0].startMs)
        assertNull(bars[0].level)             // before the first sample
        assertEquals(90, bars[3].level)
        assertTrue(bars[5].plugged)
        assertEquals(70, bars[10].level)      // the current hour
        assertNull(bars[11].level)            // still in the future
    }

    @Test fun sampleGapsListOnlyLongSilences() {
        fun at(t: Long) = LevelSample(t, 50, plugged = false)
        val min = 60_000L
        val gaps = sampleGaps(listOf(at(0), at(10 * min), at(20 * min), at(200 * min), at(210 * min)))
        assertEquals(listOf(20 * min to 200 * min), gaps)
    }
}
