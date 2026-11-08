package com.joulearc.app.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Reads per-app usage time from usage events. Needs the special "Usage access" permission. */
class AppUsageReader(context: Context) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager

    /** Foreground and foreground-service time per app, clipped to [fromMs, toMs]. */
    fun read(fromMs: Long, toMs: Long): List<RawAppUsage> {
        val manager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(fromMs, toMs)
        val aggregator = UsageEventAggregator(toMs)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventKind.ACTIVITY_START
                UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventKind.ACTIVITY_STOP
                UsageEvents.Event.FOREGROUND_SERVICE_START -> UsageEventKind.SERVICE_START
                UsageEvents.Event.FOREGROUND_SERVICE_STOP -> UsageEventKind.SERVICE_STOP
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventKind.SCREEN_OFF
                UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventKind.SHUTDOWN
                else -> null
            } ?: continue
            aggregator.onEvent(event.packageName, kind, event.timeStamp)
        }
        aggregator.finish()

        val packages = aggregator.foregroundMs.keys + aggregator.backgroundMs.keys
        return packages.mapNotNull { pkg ->
            if (pkg == appContext.packageName) return@mapNotNull null
            val fg = aggregator.foregroundMs[pkg] ?: 0L
            val bg = aggregator.backgroundMs[pkg] ?: 0L
            if (fg + bg <= 0L) null else RawAppUsage(pkg, labelOf(pkg), fg, bg)
        }
    }

    fun icon(packageName: String): Drawable? =
        try {
            pm.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun labelOf(packageName: String): String =
        try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
}
