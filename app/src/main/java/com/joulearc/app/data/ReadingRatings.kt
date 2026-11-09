package com.joulearc.app.data

import com.joulearc.app.core.Tone

enum class RatingLabel {
    TOO_LOW, LOW, NORMAL, HIGH, TOO_HIGH,
    TOO_COLD, COLD, WARM, HOT, TOO_HOT,
    CRITICAL, GOOD, FULLISH,
    SLOW, FAST, VERY_FAST, ULTRA_FAST,
    LIGHT_USE, NORMAL_USE, HEAVY_USE, VERY_HEAVY_USE,
    FAIR, WORN, POOR,
}

/** Values below [upTo] get this label. [mark] is how the boundary is shown on screen. */
data class Band(val upTo: Float, val label: RatingLabel, val tone: Tone, val mark: Float = upTo)

/**
 * A reading scale split into bands. On screen every band takes the same width, so narrow but
 * important ranges (like 5 to 10 W) stay readable; [fraction] maps a value to that layout.
 */
data class Scale(val min: Float, val max: Float, val bands: List<Band>) {
    fun rate(value: Float): Band = bands.firstOrNull { value < it.upTo } ?: bands.last()

    fun fraction(value: Float): Float {
        val n = bands.size
        var lower = min
        bands.forEachIndexed { i, band ->
            val upper = if (i == n - 1) max else band.upTo
            if (value < upper || i == n - 1) {
                val t = if (upper > lower) ((value - lower) / (upper - lower)).coerceIn(0f, 1f) else 0f
                return (i + t) / n
            }
            lower = upper
        }
        return 1f
    }
}

/** Rules of thumb used to label readings. They are guidance, not measurements. */
object Scales {
    private const val END = Float.MAX_VALUE

    /** Battery voltage in volts. */
    val voltage = Scale(
        3.0f, 4.7f,
        listOf(
            Band(3.4f, RatingLabel.LOW, Tone.CAUTION),
            Band(4.25f, RatingLabel.NORMAL, Tone.GOOD),
            Band(4.5f, RatingLabel.HIGH, Tone.INFO),
            Band(END, RatingLabel.TOO_HIGH, Tone.BAD),
        ),
    )

    /** Battery temperature in °C. */
    val temperature = Scale(
        -10f, 55f,
        listOf(
            Band(0f, RatingLabel.TOO_COLD, Tone.BAD),
            Band(15f, RatingLabel.COLD, Tone.INFO),
            Band(35f, RatingLabel.NORMAL, Tone.GOOD),
            Band(40f, RatingLabel.WARM, Tone.INFO),
            Band(45f, RatingLabel.HOT, Tone.CAUTION),
            Band(END, RatingLabel.TOO_HOT, Tone.BAD),
        ),
    )

    /** Battery level in %. */
    val level = Scale(
        0f, 100f,
        listOf(
            Band(5f, RatingLabel.CRITICAL, Tone.BAD),
            Band(20f, RatingLabel.LOW, Tone.CAUTION),
            Band(81f, RatingLabel.GOOD, Tone.GOOD, mark = 80f),
            Band(END, RatingLabel.FULLISH, Tone.INFO),
        ),
    )

    /** Power going into the battery, in watts. */
    val chargingPower = Scale(
        0f, 150f,
        listOf(
            Band(5f, RatingLabel.SLOW, Tone.CAUTION),
            Band(10f, RatingLabel.NORMAL, Tone.GOOD),
            Band(25f, RatingLabel.FAST, Tone.GOOD),
            Band(65f, RatingLabel.VERY_FAST, Tone.GOOD),
            Band(END, RatingLabel.ULTRA_FAST, Tone.GOOD),
        ),
    )

    /** Power coming out of the battery, in watts. */
    val dischargePower = Scale(
        0f, 10f,
        listOf(
            Band(1f, RatingLabel.LIGHT_USE, Tone.GOOD),
            Band(3f, RatingLabel.NORMAL_USE, Tone.GOOD),
            Band(6f, RatingLabel.HEAVY_USE, Tone.CAUTION),
            Band(END, RatingLabel.VERY_HEAVY_USE, Tone.BAD),
        ),
    )

    /** Estimated battery health in %. */
    val health = Scale(
        50f, 100f,
        listOf(
            Band(70f, RatingLabel.POOR, Tone.BAD),
            Band(80f, RatingLabel.WORN, Tone.CAUTION),
            Band(90f, RatingLabel.FAIR, Tone.INFO),
            Band(END, RatingLabel.GOOD, Tone.GOOD),
        ),
    )
}
