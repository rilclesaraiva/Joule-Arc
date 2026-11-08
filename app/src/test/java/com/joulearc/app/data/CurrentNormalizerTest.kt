package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentNormalizerTest {
    @Test fun microampsBecomeMilliamps() = assertEquals(1850, normalizeCurrentMa(1_850_000L))
    @Test fun negativeValuesAreAbsolute() = assertEquals(450, normalizeCurrentMa(-450_000L))
    @Test fun milliampsStayAsIs() = assertEquals(900, normalizeCurrentMa(900L))
    @Test fun zeroIsUnavailable() = assertNull(normalizeCurrentMa(0L))
    @Test fun minValueIsUnavailable() = assertNull(normalizeCurrentMa(Long.MIN_VALUE))
}
