package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecordedAndExtremesTest {
    @Test fun recordsChargingAndDrainingSeparately() {
        val r = RecordedCurrents()
            .update(2000, charging = true, draining = false)
            .update(500, charging = true, draining = false)
            .update(-1300, charging = false, draining = true)
            .update(-20, charging = false, draining = true)
            .update(5, charging = true, draining = false) // too small, ignored
        assertEquals(2000, r.chargeMaxMa)
        assertEquals(500, r.chargeMinMa)
        assertEquals(1300, r.drainMaxMa)
        assertEquals(20, r.drainMinMa)
    }

    @Test fun extremesTrackMinAndMax() {
        val e = ReadingExtremes()
            .update(snap(0, voltageMv = 3900, temp = 30f, current = 2000))
            .update(snap(1, voltageMv = 4100, temp = 35f, current = 1000))
            .update(snap(2, status = ChargeStatus.DISCHARGING, source = PowerSource.BATTERY, voltageMv = 3800, temp = 33f, current = 500))
        assertEquals(3800, e.minVoltageMv)
        assertEquals(4100, e.maxVoltageMv)
        assertEquals(35f, e.maxTempC!!, 0.01f)
        assertEquals(7800, e.maxChargeMw) // 3900 mV x 2000 mA
        assertEquals(1900, e.maxDrainMw)
    }

    @Test fun drainNotTrackedWhileCharging() {
        assertNull(ReadingExtremes().update(snap(0)).maxDrainMw)
    }
}
