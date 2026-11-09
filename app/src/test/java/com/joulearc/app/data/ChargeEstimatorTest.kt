package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargeEstimatorTest {
    @Test fun estimatesRateAndTimeToFull() {
        val estimator = ChargeEstimator()
        // 5000 mAh battery at 50%: counter = 2500 mAh = 2_500_000 uAh, charging at 2500 mA
        val e = estimator.update(snap(0, level = 50, current = 2500, counterUah = 2_500_000))
        assertEquals(5000, e.capacityMah)
        assertEquals(50f, e.percentPerHour!!, 0.1f)
        assertEquals(60, e.minutesToFull)
    }

    @Test fun noEstimateWithoutChargeCounter() {
        val e = ChargeEstimator().update(snap(0, counterUah = null))
        assertNull(e.percentPerHour)
        assertNull(e.minutesToFull)
    }

    @Test fun drainingEstimatesTimeLeftNotTimeToFull() {
        // 5000 mAh battery at 50%: 2500 mAh left, draining 500 mA => 5 h left, 10 %/h
        val e = ChargeEstimator().update(
            snap(0, status = ChargeStatus.DISCHARGING, source = PowerSource.BATTERY, level = 50, current = 500, counterUah = 2_500_000),
        )
        assertNull(e.minutesToFull)
        assertEquals(300, e.minutesLeft)
        assertEquals(10f, e.percentPerHour!!, 0.1f)
    }

    @Test fun pausedWhilePluggedHasNoRates() {
        val e = ChargeEstimator().update(snap(0, status = ChargeStatus.NOT_CHARGING, counterUah = 2_500_000))
        assertNull(e.percentPerHour)
        assertNull(e.minutesLeft)
        assertEquals(5000, e.capacityMah)
    }

    @Test fun averagesRecentCurrent() {
        val estimator = ChargeEstimator(windowSize = 2)
        estimator.update(snap(0, current = 1000, counterUah = 2_500_000))
        estimator.update(snap(1, current = 2000, counterUah = 2_500_000))
        val e = estimator.update(snap(2, current = 3000, counterUah = 2_500_000))
        assertEquals(2500, e.avgCurrentMa)
    }
}
