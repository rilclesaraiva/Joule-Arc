package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentUnitLearnerTest {
    @Test fun standbyReadingInMicroampsIsNotShownAsAmps() {
        val l = CurrentUnitLearner()
        repeat(3) { l.normalize(-450_000L) } // a few ordinary readings teach that the device uses microamps
        assertEquals(15, l.normalize(-15_000L)) // 15 mA standby, not 15 000 mA
        assertEquals("uA", l.label)
    }

    @Test fun unitIsUnknownUntilSeveralBigReadings() {
        val l = CurrentUnitLearner()
        l.normalize(900_000L)
        l.normalize(900_000L)
        assertNull(l.microamps)
        assertEquals(15_000, l.normalize(15_000L)) // still the size rule
        l.normalize(900_000L)
        assertTrue(l.microamps == true)
    }

    @Test fun learnedUnitIsReportedAndRestored() {
        var saved: Boolean? = null
        val l = CurrentUnitLearner { saved = it }
        repeat(3) { l.normalize(2_000_000L) }
        assertEquals(true, saved)
        assertEquals(7, CurrentUnitLearner(initial = true).normalize(7_000L))
    }

    @Test fun milliampDeviceNeverFlips() {
        val l = CurrentUnitLearner()
        repeat(50) { l.normalize(1_200L) }
        assertNull(l.microamps)
        assertEquals(15, l.normalize(15L))
    }
}
