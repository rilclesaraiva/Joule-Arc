package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertTrackerTest {
    @Test fun chargeReminderFiresOncePerCharge() {
        val t = AlertTracker()
        assertTrue(t.update(79, plugged = true, chargeAt = 80, lowAt = null).isEmpty())
        assertEquals(listOf(AlertKind.CHARGE_LIMIT), t.update(80, true, 80, null))
        assertTrue(t.update(81, true, 80, null).isEmpty())
        assertTrue(t.update(80, true, 80, null).isEmpty())
        t.update(80, plugged = false, chargeAt = 80, lowAt = null) // unplug re-arms
        assertEquals(listOf(AlertKind.CHARGE_LIMIT), t.update(80, true, 80, null))
    }

    @Test fun lowReminderFiresOnceUntilCharged() {
        val t = AlertTracker()
        assertTrue(t.update(16, plugged = false, chargeAt = null, lowAt = 15).isEmpty())
        assertEquals(listOf(AlertKind.LOW_BATTERY), t.update(15, false, null, 15))
        assertTrue(t.update(14, false, null, 15).isEmpty())
        t.update(14, plugged = true, chargeAt = null, lowAt = 15)
        assertEquals(listOf(AlertKind.LOW_BATTERY), t.update(13, false, null, 15))
    }

    @Test fun disabledRemindersNeverFire() {
        assertTrue(AlertTracker().update(100, true, null, null).isEmpty())
    }
}
