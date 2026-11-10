package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

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

    @Test fun levelStoreKeepsCurrentVoltageAndTemperature() {
        val file = File.createTempFile("levels", ".json")
        val store = LevelStore(file)
        store.save(listOf(
            LevelSample(1L, 80, true, true, currentMa = -221, voltageMv = 3917, tempC = 33.0f),
            LevelSample(2L, 79, false, false),
        ))
        val back = store.load()
        assertEquals(-221, back[0].currentMa)
        assertEquals(3917, back[0].voltageMv)
        assertEquals(33.0f, back[0].tempC!!, 0.01f)
        assertNull(back[1].currentMa)
        file.delete()
    }

    @Test fun oldLevelFilesWithFourColumnsStillLoad() {
        val file = File.createTempFile("levels", ".json")
        file.writeText("[[1000,70,0,1],[2000,69,0,0]]")
        val back = LevelStore(file).load()
        assertEquals(2, back.size)
        assertNull(back[0].voltageMv)
        file.delete()
    }
}
