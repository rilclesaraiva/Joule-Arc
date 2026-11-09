package com.joulearc.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmberGateTest {
    @Test fun notConfirmedUntilOddLastsLongEnough() {
        val gate = AmberGate(holdMs = 45_000)
        assertFalse(gate.confirmed(true, 0))
        assertFalse(gate.confirmed(true, 44_000))
        assertTrue(gate.confirmed(true, 45_000))
    }

    @Test fun normalReadingRestartsTheClock() {
        val gate = AmberGate(holdMs = 45_000)
        gate.confirmed(true, 0)
        assertFalse(gate.confirmed(false, 30_000))
        assertFalse(gate.confirmed(true, 40_000))
        assertFalse(gate.confirmed(true, 80_000))
        assertTrue(gate.confirmed(true, 85_000))
    }

    @Test fun normalReadingIsNeverConfirmed() {
        assertFalse(AmberGate().confirmed(false, 1_000_000))
    }
}
