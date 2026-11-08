package com.joulearc.app.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Reads per-app usage time. Needs the special "Usage access" permission. */
class AppUsageReader(context: Context) {
    private val appContext = context.applicationContext

    fun read(windowMs: Long): List<RawAppUsage> {
        val manager = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val stats = manager.queryAndAggregateUsageStats(end - windowMs, end)
        val pm = appContext.packageManager
        return stats.values.mapNotNull { u ->
            if (u.packageName == appContext.packageName) return@mapNotNull null
            val fg = u.totalTimeInForeground
            val bg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                u.totalTimeForegroundServiceUsed
            } else 0L
            if (fg + bg <= 0L) return@mapNotNull null
            RawAppUsage(u.packageName, labelOf(pm, u.packageName), fg, bg)
        }
    }

    private fun labelOf(pm: PackageManager, packageName: String): String =
        try {
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
}
