package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SignedCurrentTest {
    @Test fun dischargingIsNegative() {
        val s = snap(0, status = ChargeStatus.DISCHARGING, source = PowerSource.BATTERY, current = 458)
        assertEquals(-458, s.signedCurrentMa)
        assertEquals(-1832, s.signedPowerMw) // 4000 mV x 458 mA
    }

    @Test fun chargingIsPositive() {
        val s = snap(0, current = 1850)
        assertEquals(1850, s.signedCurrentMa)
        assertEquals(7400, s.signedPowerMw)
    }

    @Test fun unknownCurrentStaysNull() {
        assertNull(snap(0, current = null).signedCurrentMa)
    }
}
