package com.joulearc.app.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.text.DecimalFormat
import kotlin.math.roundToInt

/** Signed power in watts, from milliwatts, e.g. "+11.50 W". */
fun formatPower(mw: Int): String = String.format(Locale.getDefault(), "%+.2f W", mw / 1000f)

fun formatWatts(mw: Int): String = String.format(Locale.getDefault(), "%.1f W", mw / 1000f)

fun formatDuration(ms: Long): String {
    val totalMin = ms / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 -> "$h h $m min"
        totalMin > 0 -> "$m min"
        else -> "${ms / 1000L} s"
    }
}

fun formatMinutes(minutes: Int): String = formatDuration(minutes * 60_000L)

fun formatDateTime(ms: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

fun formatClock(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(ms))

/** Temperature with one decimal, in °C or °F. */
fun formatTemperature(celsius: Float, fahrenheit: Boolean): String =
    if (fahrenheit) String.format(Locale.getDefault(), "%.1f °F", celsius * 9f / 5f + 32f)
    else String.format(Locale.getDefault(), "%.1f °C", celsius)

/** Whole degrees for scale marks, e.g. "35°". */
fun formatDegrees(celsius: Float, fahrenheit: Boolean): String =
    "${(if (fahrenheit) celsius * 9f / 5f + 32f else celsius).roundToInt()}°"

fun formatVolts(mv: Int, decimals: Int = 2): String =
    String.format(Locale.getDefault(), "%.${decimals}f V", mv / 1000f)

/** Number without trailing zeros, e.g. 4.25 or 5. */
fun formatTrim(value: Float): String = DecimalFormat("0.##").format(value)
