package com.joulearc.app.core

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Meaning of a reading's color: good, informative, needs attention, or bad. */
enum class Tone { GOOD, INFO, CAUTION, BAD }

/** Fixed state colors (green, blue, amber, red), tuned for light and dark backgrounds. */
data class StatusPalette(val good: Color, val info: Color, val caution: Color, val bad: Color) {
    fun of(tone: Tone): Color = when (tone) {
        Tone.GOOD -> good
        Tone.INFO -> info
        Tone.CAUTION -> caution
        Tone.BAD -> bad
    }
}

@Composable
fun statusPalette(): StatusPalette {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return if (dark) {
        StatusPalette(Color(0xFF66BB6A), Color(0xFF64B5F6), Color(0xFFFFCA28), Color(0xFFEF5350))
    } else {
        StatusPalette(Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFB26A00), Color(0xFFC62828))
    }
}
