package com.joulearc.app.data

import android.content.Context
import android.os.Build
import android.os.PowerManager

/** Device facts shown in the app and used in problem reports. Nothing here leaves the device. */
object DeviceInfo {
    val androidVersion: String get() = Build.VERSION.RELEASE ?: ""
    val manufacturer: String get() = Build.MANUFACTURER ?: ""
    val model: String get() = Build.MODEL ?: ""

    /**
     * Design capacity reported by the system power profile. This is not a public API: it is read by
     * reflection, can be missing or blocked, and some devices report a placeholder. The user can
     * always type the real value in Settings.
     */
    fun systemDesignCapacityMah(context: Context): Int? =
        try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val profile = cls.getConstructor(Context::class.java).newInstance(context.applicationContext)
            val value = cls.getMethod("getBatteryCapacity").invoke(profile) as Double
            value.toInt().takeIf { it in 1500..30_000 }
        } catch (t: Throwable) {
            null
        }

    fun appVersion(context: Context): String =
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
