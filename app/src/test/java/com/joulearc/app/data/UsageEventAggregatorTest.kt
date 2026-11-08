package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageEventAggregatorTest {
    private fun agg(vararg events: Triple<String, UsageEventKind, Long>, end: Long = 1_000L): UsageEventAggregator {
        val a = UsageEventAggregator(end)
        events.forEach { (pkg, kind, t) -> a.onEvent(pkg, kind, t) }
        a.finish()
        return a
    }

    @Test fun sumsForegroundIntervals() {
        val a = agg(
            Triple("a", UsageEventKind.ACTIVITY_START, 0), Triple("a", UsageEventKind.ACTIVITY_STOP, 100),
            Triple("a", UsageEventKind.ACTIVITY_START, 200), Triple("a", UsageEventKind.ACTIVITY_STOP, 250),
        )
        assertEquals(150L, a.foregroundMs["a"])
    }

    @Test fun openIntervalEndsAtWindowEnd() {
        val a = agg(Triple("a", UsageEventKind.ACTIVITY_START, 900), end = 1_000L)
        assertEquals(100L, a.foregroundMs["a"])
    }

    @Test fun screenOffClosesForeground() {
        val a = UsageEventAggregator(1_000L)
        a.onEvent("a", UsageEventKind.ACTIVITY_START, 100)
        a.onEvent(null, UsageEventKind.SCREEN_OFF, 300)
        a.finish()
        assertEquals(200L, a.foregroundMs["a"])
    }

    @Test fun repeatedStartKeepsEarliest() {
        val a = agg(
            Triple("a", UsageEventKind.ACTIVITY_START, 0), Triple("a", UsageEventKind.ACTIVITY_START, 50),
            Triple("a", UsageEventKind.ACTIVITY_STOP, 100),
        )
        assertEquals(100L, a.foregroundMs["a"])
    }

    @Test fun serviceTimeCountsOverlapOnce() {
        val a = agg(
            Triple("a", UsageEventKind.SERVICE_START, 0), Triple("a", UsageEventKind.SERVICE_START, 100),
            Triple("a", UsageEventKind.SERVICE_STOP, 200), Triple("a", UsageEventKind.SERVICE_STOP, 400),
        )
        assertEquals(400L, a.backgroundMs["a"])
    }

    @Test fun stopWithoutStartIsIgnored() {
        val a = agg(Triple("a", UsageEventKind.ACTIVITY_STOP, 100), Triple("a", UsageEventKind.SERVICE_STOP, 100))
        assertEquals(null, a.foregroundMs["a"])
        assertEquals(null, a.backgroundMs["a"])
    }

    @Test fun shutdownClosesEverything() {
        val a = UsageEventAggregator(1_000L)
        a.onEvent("a", UsageEventKind.ACTIVITY_START, 0)
        a.onEvent("a", UsageEventKind.SERVICE_START, 0)
        a.onEvent(null, UsageEventKind.SHUTDOWN, 100)
        a.finish()
        assertEquals(100L, a.foregroundMs["a"])
        assertEquals(100L, a.backgroundMs["a"])
    }
}
