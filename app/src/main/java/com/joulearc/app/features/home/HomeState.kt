package com.joulearc.app.features.home

import com.joulearc.app.data.BatterySnapshot
import com.joulearc.app.data.ChargeEstimate
import com.joulearc.app.data.ChargeInsights
import com.joulearc.app.data.DrainStats
import com.joulearc.app.data.ReadingExtremes
import com.joulearc.app.data.RecordedCurrents

/** Everything the home screen shows, assembled by the entry point. */
data class HomeState(
    val snapshot: BatterySnapshot?,
    val estimate: ChargeEstimate?,
    val capacityMah: Int?,
    /** Capacity reported by the system: the reference for health. */
    val designCapacityMah: Int?,
    /** Capacity the manufacturer advertises (assumed), shown beside it. */
    val manufacturerCapacityMah: Int?,
    val recorded: RecordedCurrents,
    val extremes: ReadingExtremes,
    val fahrenheit: Boolean,
    val sessionCount: Int,
    val insights: ChargeInsights,
    val drain: DrainStats,
    val notificationsGranted: Boolean,
)

/** Readings that open a detail sheet when tapped. */
enum class Reading { CURRENT, VOLTAGE, TEMPERATURE, LEVEL, POWER, STATUS, SOURCE, HEALTH, TECHNOLOGY, CYCLES }
