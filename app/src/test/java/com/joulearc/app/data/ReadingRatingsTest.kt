package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingRatingsTest {
    @Test fun ratesReadingsLikeTheReferenceApp() {
        assertEquals(RatingLabel.NORMAL, Scales.voltage.rate(3.70f).label)
        assertEquals(RatingLabel.WARM, Scales.temperature.rate(36.9f).label)
        assertEquals(RatingLabel.GOOD, Scales.level.rate(32f).label)
        assertEquals(RatingLabel.GOOD, Scales.level.rate(80f).label)
        assertEquals(RatingLabel.FULLISH, Scales.level.rate(81f).label)
        assertEquals(RatingLabel.FAST, Scales.chargingPower.rate(13.3f).label)
        assertEquals(RatingLabel.NORMAL_USE, Scales.dischargePower.rate(1.59f).label)
        assertEquals(RatingLabel.WORN, Scales.health.rate(79f).label)
        assertEquals(RatingLabel.TOO_HOT, Scales.temperature.rate(60f).label)
    }

    @Test fun bandsTakeEqualWidth() {
        // voltage has 4 bands: 3.0–3.4, 3.4–4.25, 4.25–4.5, 4.5–4.7
        assertEquals(0f, Scales.voltage.fraction(2.5f), 0.001f)
        assertEquals(0.25f, Scales.voltage.fraction(3.4f), 0.001f)
        assertEquals(0.5f, Scales.voltage.fraction(4.25f), 0.001f)
        assertEquals(0.625f, Scales.voltage.fraction(4.375f), 0.001f)
        assertEquals(1f, Scales.voltage.fraction(9f), 0.001f)
    }
}
