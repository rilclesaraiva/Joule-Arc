package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargeSessionTrackerTest {
    private val unplugged = { t: Long ->
        snap(t, status = ChargeStatus.DISCHARGING, source = PowerSource.BATTERY, current = 400)
    }

    @Test fun startsAndUpdatesSession() {
        val tracker = ChargeSessionTracker()
        assertNull(tracker.onSnapshot(snap(0, level = 40, current = 1000)))
        assertNull(tracker.onSnapshot(snap(10_000, level = 41, current = 3000)))
        val s = tracker.current!!
        assertTrue(s.ongoing)
        assertEquals(40, s.startLevel)
        assertEquals(41, s.endLevel)
        assertEquals(3000, s.peakCurrentMa)
        assertEquals(10_000L, s.durationMs)
        // time-weighted: the whole 10 s interval is credited to the new sample
        assertEquals(3000.0, s.avgCurrentMa, 0.01)
    }

    @Test fun unplugClosesSessionOnceTheResumeWindowPasses() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0, level = 40))
        tracker.onSnapshot(snap(60_000, level = 45))
        assertNull(tracker.onSnapshot(unplugged(61_000)))
        assertNull(tracker.current)
        assertNotNull(tracker.pending)
        assertTrue(tracker.pending!!.paused)
        assertNull(tracker.onSnapshot(unplugged(200_000)))
        val closed = tracker.onSnapshot(unplugged(60_000 + RESUME_WINDOW_MS + 1_000))
        assertNotNull(closed)
        assertFalse(closed!!.ongoing)
        assertEquals(60_000L, closed.durationMs)
        assertEquals(5, closed.gainPercent)
        assertNull(tracker.pending)
    }

    @Test fun reconnectingWithinFiveMinutesContinuesTheSessionWithAPause() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0, level = 40, current = 3000))
        tracker.onSnapshot(snap(60_000, level = 45, current = 3000))
        assertNull(tracker.onSnapshot(unplugged(70_000)))
        assertNull(tracker.onSnapshot(snap(180_000, level = 45, current = 3000)))
        val s = tracker.current!!
        assertNull(tracker.pending)
        assertTrue(s.ongoing)
        assertFalse(s.paused)
        assertEquals(40, s.startLevel)
        assertEquals(listOf(SessionPause(60, 120)), s.pauses)
        assertEquals(120_000L, s.pausedMs)
        assertEquals(60_000L, s.chargingMs)
        // the pause is not averaged in
        assertEquals(3000.0, s.avgCurrentMa, 0.01)
    }

    @Test fun reconnectingAfterTheWindowStartsANewSession() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0, level = 40))
        tracker.onSnapshot(snap(60_000, level = 45))
        tracker.onSnapshot(unplugged(70_000))
        val closed = tracker.onSnapshot(snap(60_000 + RESUME_WINDOW_MS + 5_000, level = 50))
        assertNotNull(closed)
        assertEquals(45, closed!!.endLevel)
        assertEquals(50, tracker.current!!.startLevel)
        assertTrue(tracker.current!!.pauses.isEmpty())
    }

    @Test fun briefStatusFlickerKeepsSessionOpen() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0))
        val flicker = snap(30_000, status = ChargeStatus.NOT_CHARGING, source = PowerSource.USB)
        assertNull(tracker.onSnapshot(flicker))
        assertNotNull(tracker.current)
        assertNull(tracker.onSnapshot(snap(40_000)))
        assertEquals(40_000L, tracker.current!!.durationMs)
    }

    @Test fun longGapClosesAndStartsNewSession() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0, level = 20))
        val closed = tracker.onSnapshot(snap(500_000, level = 60))
        assertNotNull(closed)
        assertEquals(0L, closed!!.durationMs)
        assertEquals(60, tracker.current!!.startLevel)
    }

    @Test fun percentPerHourNeedsFiveMinutes() {
        val short = ChargeSessionTracker().apply {
            onSnapshot(snap(0, level = 10)); onSnapshot(snap(60_000, level = 12))
        }.current!!
        assertNull(short.percentPerHour)

        val long = ChargeSessionTracker().apply {
            onSnapshot(snap(0, level = 10)); onSnapshot(snap(100_000, level = 10))
            onSnapshot(snap(200_000, level = 10)); onSnapshot(snap(300_000, level = 15))
        }.current!!
        assertEquals(60f, long.percentPerHour!!, 0.1f)
    }

    @Test fun recordsSparseCurvePoints() {
        val tracker = ChargeSessionTracker()
        tracker.onSnapshot(snap(0, level = 40, current = 1000))
        tracker.onSnapshot(snap(10_000, level = 40, current = 1100))  // too soon, same level: no point
        tracker.onSnapshot(snap(40_000, level = 40, current = 1200))  // 30 s passed: point
        tracker.onSnapshot(snap(45_000, level = 41, current = 1300))  // level changed: point
        val points = tracker.current!!.points
        assertEquals(listOf(0, 40, 45), points.map { it.offsetSec })
        assertEquals(listOf(40, 40, 41), points.map { it.level })
        assertEquals(1300, points.last().currentMa)
    }
}
