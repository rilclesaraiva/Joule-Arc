package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerStateTest {
    @Test fun pluggedButDrainingIsAmberAndNegative() {
        val s = snap(0, status = ChargeStatus.CHARGING, source = PowerSource.USB, current = 300).copy(pluggedDischarging = true)
        assertEquals(PowerState.PLUGGED_DRAINING, s.powerState)
        assertFalse(s.isCharging)
        assertTrue(s.drawingFromBattery)
        assertEquals(-300, s.signedCurrentMa)
    }

    @Test fun pluggedNotChargingIsPaused() {
        val s = snap(0, status = ChargeStatus.NOT_CHARGING, source = PowerSource.AC, current = 20)
        assertEquals(PowerState.PLUGGED_PAUSED, s.powerState)
        assertFalse(s.drawingFromBattery)
    }

    @Test fun fullWinsOverDraining() {
        val s = snap(0, status = ChargeStatus.FULL, source = PowerSource.AC).copy(pluggedDischarging = true)
        assertEquals(PowerState.FULL, s.powerState)
    }

    @Test fun chargerMaxPower() {
        val s = snap(0).copy(chargerMaxVoltageMv = 22_000, chargerMaxCurrentMa = 3_220)
        assertEquals(70_840, s.chargerMaxMw)
    }

    @Test fun settlingChargerIsCheckingNotAmber() {
        val s = snap(0, status = ChargeStatus.CHARGING, source = PowerSource.USB, current = 300)
            .copy(pluggedDischarging = true, settling = true)
        assertEquals(PowerState.CHECKING, s.powerState)
        assertFalse(s.isCharging)
        assertEquals(-300, s.signedCurrentMa)
    }

    @Test fun settlingDoesNotHideFull() {
        val s = snap(0, status = ChargeStatus.FULL, source = PowerSource.AC).copy(settling = true)
        assertEquals(PowerState.FULL, s.powerState)
    }
}
