package com.joulearc.app.data

import android.content.Context
import android.content.res.Resources
import org.xmlpull.v1.XmlPullParser
import android.os.Build
import android.os.PowerManager
import java.io.File

/** Where a detected capacity came from, shown to the user so a wrong value can be traced. */
enum class CapacitySource { POWER_PROFILE, POWER_PROFILE_XML, KERNEL }

data class DetectedCapacity(val mah: Int, val source: CapacitySource)

/** Raw result of one capacity source (null when it failed) and a short technical note. */
data class SourceReading(val source: CapacitySource, val rawMah: Int?, val note: String?)

/** Device facts shown in the app and used in problem reports. Nothing here leaves the device. */
object DeviceInfo {
    val androidVersion: String get() = Build.VERSION.RELEASE ?: ""
    val manufacturer: String get() = Build.MANUFACTURER ?: ""
    val model: String get() = Build.MODEL ?: ""
    val apiLevel: Int get() = Build.VERSION.SDK_INT
    val securityPatch: String get() = Build.VERSION.SECURITY_PATCH ?: ""
    val deviceCode: String get() = Build.DEVICE ?: ""
    val brand: String get() = Build.BRAND ?: ""
    val product: String get() = Build.PRODUCT ?: ""
    val hardware: String get() = Build.HARDWARE ?: ""
    val board: String get() = Build.BOARD ?: ""
    val fingerprint: String get() = Build.FINGERPRINT ?: ""

    /**
     * Device capacity detected by the app itself: the first plausible value among the system power
     * profile (not a public API, read by reflection), the same profile read as a framework XML
     * resource, and the kernel battery node. Any of them can be missing, blocked or a placeholder, so
     * the result is only a starting point: the value the user types in Settings always wins.
     */
    fun detectDesignCapacity(context: Context): DetectedCapacity? =
        readCapacitySources(context)
            .firstOrNull { r -> r.rawMah?.let { it in PLAUSIBLE_MAH } == true }
            ?.let { DetectedCapacity(it.rawMah!!, it.source) }

    /** Every source with its raw result or the reason it failed. */
    fun readCapacitySources(context: Context): List<SourceReading> =
        listOf(readPowerProfile(context), readPowerProfileXml(), readKernelNode())

    private fun readPowerProfile(context: Context): SourceReading =
        try {
            val cls = Class.forName("com.android.internal.os.PowerProfile")
            val profile = cls.getConstructor(Context::class.java).newInstance(context.applicationContext)
            val direct = try {
                cls.getMethod("getBatteryCapacity").invoke(profile) as Double
            } catch (t: Throwable) {
                null
            }
            val average = try {
                cls.getMethod("getAveragePower", String::class.java).invoke(profile, "battery.capacity") as Double
            } catch (t: Throwable) {
                null
            }
            // getBatteryCapacity() can return a figure adjusted by the system (4079 on a 5000 mAh phone);
            // the "battery.capacity" entry of the power profile is the rated one, which is what we want.
            val value = average
            SourceReading(
                CapacitySource.POWER_PROFILE,
                value?.toInt(),
                "getBatteryCapacity=${direct ?: "x"} getAveragePower=${average ?: "x"}",
            )
        } catch (t: Throwable) {
            SourceReading(CapacitySource.POWER_PROFILE, null, failure(t))
        }

    private fun readPowerProfileXml(): SourceReading =
        try {
            val res = Resources.getSystem()
            val id = res.getIdentifier("power_profile", "xml", "android")
            if (id == 0) {
                SourceReading(CapacitySource.POWER_PROFILE_XML, null, "resource not found")
            } else {
                val parser = res.getXml(id)
                try {
                    var value: Double? = null
                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT && value == null) {
                        if (event == XmlPullParser.START_TAG && parser.name == "item" &&
                            parser.getAttributeValue(null, "name") == "battery.capacity"
                        ) {
                            value = parser.nextText().trim().toDoubleOrNull()
                        }
                        event = parser.next()
                    }
                    SourceReading(CapacitySource.POWER_PROFILE_XML, value?.toInt(), if (value == null) "item not found" else null)
                } finally {
                    parser.close()
                }
            }
        } catch (t: Throwable) {
            SourceReading(CapacitySource.POWER_PROFILE_XML, null, failure(t))
        }

    /** Kernel value in µAh (some devices use mAh). Usually unreadable by apps, so a failure is normal. */
    private fun readKernelNode(): SourceReading {
        val paths = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design",
        )
        var note: String? = null
        for (path in paths) {
            try {
                val raw = File(path).readText().trim().toLongOrNull() ?: continue
                val mah = if (raw > 100_000L) raw / 1000L else raw
                return SourceReading(CapacitySource.KERNEL, mah.toInt(), null)
            } catch (t: Throwable) {
                note = failure(t)
            }
        }
        return SourceReading(CapacitySource.KERNEL, null, note)
    }

    private fun failure(t: Throwable): String {
        val cause = t.cause ?: t
        return (cause.javaClass.simpleName + ": " + (cause.message ?: "")).take(80)
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
