package com.joulearc.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentSignLearnerTest {
    private fun CurrentSignLearner.discharge(sign: Int, times: Int = 5) =
        repeat(times) { observe(sign, knownDischarging = true, magnitudeMa = 400) }

    private fun CurrentSignLearner.charge(sign: Int, times: Int = 5) =
        repeat(times) { observe(sign, knownDischarging = false, magnitudeMa = 2000, knownCharging = true) }

    @Test fun trustsSignOnlyAfterSeeingBothSigns() {
        val l = CurrentSignLearner()
        l.discharge(-1)
        assertFalse(l.isDischarge(-1, 400)) // only one sign seen so far
        l.observe(sign = 1, knownDischarging = false, magnitudeMa = 2000)
        assertTrue(l.isDischarge(-1, 400))
        assertFalse(l.isDischarge(1, 400))
        assertTrue(l.isCharge(1, 400))
    }

    @Test fun deviceThatNeverChangesSignNeverFlagsDraining() {
        val l = CurrentSignLearner()
        l.discharge(1)
        l.charge(1) // a steady charge with the same sign votes against it
        assertFalse(l.isDischarge(1, 400))
        assertFalse(l.isCharge(1, 400))
    }

    @Test fun ignoresTinyCurrents() {
        val l = CurrentSignLearner()
        l.discharge(-1)
        l.observe(1, false, 400)
        assertFalse(l.isDischarge(-1, 10))
    }

    @Test fun oneStaleReadingAfterUnplugDoesNotFlipTheSign() {
        val l = CurrentSignLearner()
        l.discharge(-1, times = 20)
        l.observe(1, knownDischarging = false, magnitudeMa = 1500)
        l.discharge(1, times = 1) // the stale charging reading right after unplugging
        assertTrue(l.isDischarge(-1, 400))
        assertFalse(l.isDischarge(1, 1000))
    }

    @Test fun steadyChargeTeachesTheSignWithoutAnyUnplug() {
        val l = CurrentSignLearner()
        l.observe(-1, knownDischarging = false, magnitudeMa = 200) // seen once, for example a load spike
        l.charge(1)
        assertTrue(l.isCharge(1, 1000))
        assertTrue(l.isDischarge(-1, 1000))
    }
}
