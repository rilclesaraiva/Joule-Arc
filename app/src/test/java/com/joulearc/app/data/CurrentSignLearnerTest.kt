package com.joulearc.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentSignLearnerTest {
    @Test fun trustsSignOnlyAfterSeeingBothSigns() {
        val l = CurrentSignLearner()
        l.observe(sign = -1, knownDischarging = true, magnitudeMa = 400)
        assertFalse(l.isDischarge(-1, 400)) // only one sign seen so far
        l.observe(sign = 1, knownDischarging = false, magnitudeMa = 2000)
        assertTrue(l.isDischarge(-1, 400))
        assertFalse(l.isDischarge(1, 400))
    }

    @Test fun deviceThatNeverChangesSignNeverFlagsDraining() {
        val l = CurrentSignLearner()
        l.observe(1, knownDischarging = true, magnitudeMa = 400)
        l.observe(1, knownDischarging = false, magnitudeMa = 2000)
        assertFalse(l.isDischarge(1, 400))
    }

    @Test fun ignoresTinyCurrents() {
        val l = CurrentSignLearner()
        l.observe(-1, true, 400)
        l.observe(1, false, 400)
        assertFalse(l.isDischarge(-1, 10))
    }
}
