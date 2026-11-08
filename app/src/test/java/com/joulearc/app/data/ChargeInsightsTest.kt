package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeInsightsTest {
    private fun session(
        startMs: Long,
        avg: Double,
        source: PowerSource,
        durationMs: Long = 600_000L,
        startLevel: Int = 30,
        ongoing: Boolean = false,
    ) = ChargeSession(
        startMs = startMs, lastMs = startMs + durationMs, startLevel = startLevel, endLevel = startLevel + 10,
        peakCurrentMa = avg.toInt(), avgCurrentMa = avg, peakPowerMw = 0, maxTempC = 30f,
        source = source, ongoing = ongoing,
    )

    // startMs is used as the hour directly in these tests
    private val hourOf = { ms: Long -> ms.toInt() }

    @Test fun findsFastestSourceAndPeriod() {
        val sessions = listOf(
            session(2, 3000.0, PowerSource.AC), session(3, 3200.0, PowerSource.AC),
            session(14, 1000.0, PowerSource.USB), session(15, 1200.0, PowerSource.USB),
        )
        val r = computeInsights(sessions, hourOf)
        assertEquals(PowerSource.AC, r.bestSource)
        assertEquals(DayPeriod.NIGHT, r.bestPeriod)
        assertEquals(3100, r.bySource.first().avgCurrentMa)
        assertEquals(4, r.usedSessions)
    }

    @Test fun ignoresShortOngoingAndHighStartLevel() {
        val sessions = listOf(
            session(1, 3000.0, PowerSource.AC, durationMs = 60_000L),
            session(2, 3000.0, PowerSource.AC, ongoing = true),
            session(3, 3000.0, PowerSource.AC, startLevel = 90),
        )
        assertEquals(0, computeInsights(sessions, hourOf).usedSessions)
    }

    @Test fun noWinnerWithSingleOptionOrSingleSession() {
        val one = computeInsights(listOf(session(1, 2000.0, PowerSource.AC), session(2, 2100.0, PowerSource.AC)), hourOf)
        assertNull(one.bestSource)
        val few = computeInsights(listOf(session(1, 3000.0, PowerSource.AC), session(2, 1000.0, PowerSource.USB)), hourOf)
        assertNull(few.bestSource)
    }

    @Test fun weightsByDuration() {
        val sessions = listOf(
            session(1, 1000.0, PowerSource.AC, durationMs = 300_000L),
            session(2, 4000.0, PowerSource.AC, durationMs = 900_000L),
        )
        assertEquals(3250, computeInsights(sessions, hourOf).bySource.single().avgCurrentMa)
    }

    @Test fun periodsByHour() {
        assertEquals(DayPeriod.NIGHT, DayPeriod.ofHour(5))
        assertEquals(DayPeriod.MORNING, DayPeriod.ofHour(6))
        assertEquals(DayPeriod.AFTERNOON, DayPeriod.ofHour(12))
        assertEquals(DayPeriod.EVENING, DayPeriod.ofHour(23))
    }
}
