package com.joulearc.app.core

import java.text.DateFormat
import java.util.Date
import java.util.Locale

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
