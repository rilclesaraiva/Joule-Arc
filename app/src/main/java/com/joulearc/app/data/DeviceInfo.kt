package com.joulearc.app.data

import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.io.File

/** Device facts shown in the app and used in problem reports. Nothing here leaves the device. */
object DeviceInfo {
    val androidVersion: String get() = Build.VERSION.RELEASE ?: ""
    val manufacturer: String get() = Build.MANUFACTURER ?: ""
    val model: String get() = Build.MODEL ?: ""

    /**
     * Design capacity detected by the app itself, trying each source in turn: the system power
     * profile (not a public API, read by reflection) and, on devices that allow it, the kernel
     * battery node. Any of them can be missing, blocked or a placeholder, so the result is only a
     * starting point: the value the user types in Settings always wins.
     */
    fun systemDesignCapacityMah(context: Context): Int? =
        fromPowerProfile(context) ?: fromKernelNode()

    private fun fromPowerProfile(context: Context): Int? =
        try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val profile = cls.getConstructor(Context::class.java).newInstance(context.applicationContext)
            val direct = try {
                cls.getMethod("getBatteryCapacity").invoke(profile) as Double
            } catch (t: Throwable) {
                0.0
            }
            val value = if (direct > 0.0) {
                direct
            } else {
                cls.getMethod("getAveragePower", String::class.java).invoke(profile, "battery.capacity") as Double
            }
            value.toInt().takeIf { it in PLAUSIBLE_MAH }
        } catch (t: Throwable) {
            null
        }

    /** Kernel value in µAh (some devices use mAh). Usually unreadable by apps, so a failure is normal. */
    private fun fromKernelNode(): Int? {
        val paths = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design",
        )
        for (path in paths) {
            val raw = try {
                File(path).readText().trim().toLongOrNull()
            } catch (t: Throwable) {
                null
            } ?: continue
            val mah = if (raw > 100_000L) raw / 1000L else raw
            if (mah.toInt() in PLAUSIBLE_MAH) return mah.toInt()
        }
        return null
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

    private val PLAUSIBLE_MAH = 1500..30_000
}
