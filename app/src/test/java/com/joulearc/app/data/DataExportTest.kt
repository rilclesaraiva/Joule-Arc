package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DataExportTest {
    private val hour = 3_600_000L

    private fun s(t: Long, screenOn: Boolean) = LevelSample(t, 50, plugged = false, screenOn = screenOn)

    @Test fun screenTimesSplitByStateAndClipToTheWindow() {
        val samples = listOf(s(0, true), s(hour, false), s(3 * hour, true), s(4 * hour, true))
        val r = DataExport.screenTimes(samples, hour / 2, 3 * hour + hour / 2)
        // 0.5h..1h on, 1h..3h off, 3h..3.5h on
        assertEquals(hour, r.onMs)
        assertEquals(2 * hour, r.offMs)
        assertEquals(3 * hour, r.coveredMs)
    }

    @Test fun screenTimesSkipLongGaps() {
        val r = DataExport.screenTimes(listOf(s(0, true), s(5 * hour, true)), 0, 5 * hour)
        assertEquals(0L, r.coveredMs)
    }
}
