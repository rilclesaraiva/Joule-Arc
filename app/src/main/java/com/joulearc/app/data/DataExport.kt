package com.joulearc.app.data

import android.content.Context
import android.util.JsonWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** What the export needs from the running app (the rest is read from the repositories). */
data class ExportInput(
    val snapshot: BatterySnapshot?,
    val estimate: ChargeEstimate?,
    val capacityMah: Int?,
    val designCapacityMah: Int?,
    val usageGranted: Boolean,
)

/**
 * Writes everything the app knows to one JSON file, for people who want to analyze a drain
 * themselves: the device, the battery now, the level history, every charging session, the
 * consumption rates and the time each app was used (whole period, per day and per hour of the last
 * day). It is only written when the user asks, to a place the user picks; nothing is sent anywhere.
 */
object DataExport {
    private const val SCHEMA = 2
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS
    private const val MAX_GAP_MS = 2 * HOUR_MS

    fun fileName(nowMs: Long): String =
        "joule-arc-export_" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date(nowMs)) + ".json"

    fun write(context: Context, out: OutputStream, input: ExportInput, nowMs: Long = System.currentTimeMillis()) {
        val w = JsonWriter(OutputStreamWriter(out, Charsets.UTF_8))
        w.beginObject()
        meta(w, context, nowMs)
        device(w)
        battery(w, context, input)
        settings(w)
        val samples = LevelHistoryRepository.samples.value
        levelHistory(w, samples)
        sessions(w)
        drain(w, samples, nowMs)
        apps(w, context, input, samples, nowMs)
        w.endObject()
        w.flush()
    }

    private fun meta(w: JsonWriter, context: Context, nowMs: Long) {
        val tz = TimeZone.getDefault()
        w.name("meta").beginObject()
        w.name("schema").value(SCHEMA)
        w.name("app").value("Joule Arc")
        w.name("appVersion").value(DeviceInfo.appVersion(context))
        w.name("exportedAtMs").value(nowMs)
        w.name("exportedAtLocal").value(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date(nowMs)))
        w.name("timeZone").value(tz.id)
        w.name("utcOffsetMinutes").value(tz.getOffset(nowMs) / 60_000)
        w.name("notes").beginArray()
        listOf(
            "All times are epoch milliseconds (UTC); use utcOffsetMinutes for local time.",
            "Currents are in mA: positive into the battery, negative out of it, where signed. Sessions and sample-free fields use positive magnitudes.",
            "Per-app battery percentages are estimates: the drained percentage of the period (from the level history) shared between apps by usage time, with background (foreground service) time weighing half. Android does not give regular apps per-app mAh.",
            "Usage times for short windows can be lower than real for apps already open when the window began.",
            "Level history is kept for 14 days (from the first sample, see levelHistory.firstSampleMs); only the 30 newest charging sessions keep their curve. Samples saved by older versions have no current, voltage or temperature.",
            "Each app window has requested bounds (fromMs/toMs). Usage times cover the whole window when Android still has the events (it keeps them for a few days). The drained percentage, drainedMah, screen times and the estimated shares cover only the part with level history (historyFromMs); partialHistory says when that is less than the window.",
            "currentUnit is the unit the device reports CURRENT_NOW in; the app converts everything to mA.",
        ).forEach { w.value(it) }
        w.endArray()
        w.endObject()
    }

    private fun device(w: JsonWriter) {
        w.name("device").beginObject()
        w.name("manufacturer").value(DeviceInfo.manufacturer)
        w.name("brand").value(DeviceInfo.brand)
        w.name("model").value(DeviceInfo.model)
        w.name("device").value(DeviceInfo.deviceCode)
        w.name("product").value(DeviceInfo.product)
        w.name("hardware").value(DeviceInfo.hardware)
        w.name("board").value(DeviceInfo.board)
        w.name("androidRelease").value(DeviceInfo.androidVersion)
        w.name("apiLevel").value(DeviceInfo.apiLevel)
        w.name("securityPatch").value(DeviceInfo.securityPatch)
        w.name("buildFingerprint").value(DeviceInfo.fingerprint)
        w.name("locale").value(Locale.getDefault().toLanguageTag())
        w.endObject()
    }

    private fun battery(w: JsonWriter, context: Context, input: ExportInput) {
        val s = input.snapshot
        w.name("battery").beginObject()
        w.name("now")
        if (s == null) w.nullValue() else {
            w.beginObject()
            w.name("timestampMs").value(s.timestampMs)
            w.name("levelPercent").value(s.levelPercent)
            w.name("status").value(s.status.name)
            w.name("powerState").value(s.powerState.name)
            w.name("source").value(s.source.name)
            w.field("currentMa", s.signedCurrentMa)
            w.name("currentUnit").value(BatteryReader.unitLearner.label)
            w.field("powerMw", s.signedPowerMw)
            w.name("voltageMv").value(s.voltageMv)
            w.name("temperatureC").value(s.temperatureC.toDouble())
            w.field("chargeCounterUah", s.chargeCounterUah)
            w.name("health").value(s.healthState.name)
            w.field("technology", s.technology)
            w.field("cycleCount", s.cycleCount)
            w.field("chargerMaxVoltageMv", s.chargerMaxVoltageMv)
            w.field("chargerMaxCurrentMa", s.chargerMaxCurrentMa)
            w.name("screenOn").value(s.screenOn)
            w.endObject()
        }
        val e = input.estimate
        w.name("estimate").beginObject()
        w.field("direction", when { s == null -> null; s.drawingFromBattery -> "LOSING_CHARGE"; s.powerState == PowerState.CHARGING -> "GAINING_CHARGE"; else -> "NONE" })
        w.field("percentPerHourMagnitude", e?.percentPerHour?.toDouble())
        w.field("minutesToFull", e?.minutesToFull)
        w.field("avgCurrentMa", e?.avgCurrentMa)
        w.endObject()
        w.name("capacity").beginObject()
        w.field("estimatedFullMah", input.capacityMah)
        w.field("deviceCapacityMahUsed", input.designCapacityMah)
        w.field("deviceCapacityMahTyped", SettingsRepository.designCapacityMah.value)
        w.field("healthPercent", estimateHealthPercent(input.capacityMah, input.designCapacityMah))
        w.name("detectionSources").beginArray()
        DeviceInfo.readCapacitySources(context).forEach {
            w.beginObject()
            w.name("source").value(it.source.name)
            w.field("rawMah", it.rawMah)
            w.field("note", it.note)
            w.endObject()
        }
        w.endArray()
        w.endObject()
        val r = SettingsRepository.recorded.value
        w.name("recordedCurrentsMa").beginObject()
        w.field("chargeMax", r.chargeMaxMa)
        w.field("chargeMin", r.chargeMinMa)
        w.field("drainMax", r.drainMaxMa)
        w.field("drainMin", r.drainMinMa)
        w.endObject()
        w.endObject()
    }

    private fun settings(w: JsonWriter) {
        w.name("settings").beginObject()
        w.name("dualCellBattery").value(SettingsRepository.dualCellBattery.value)
        w.name("fahrenheit").value(SettingsRepository.useFahrenheit.value)
        w.name("themeMode").value(SettingsRepository.themeMode.value)
        w.name("persistentNotification").value(SettingsRepository.persistentNotification.value)
        w.name("predictiveBack").value(SettingsRepository.predictiveBack.value)
        w.name("chargeAlert").beginObject()
        w.name("enabled").value(SettingsRepository.chargeAlertEnabled.value)
        w.name("level").value(SettingsRepository.chargeAlertLevel.value)
        w.endObject()
        w.name("lowAlert").beginObject()
        w.name("enabled").value(SettingsRepository.lowAlertEnabled.value)
        w.name("level").value(SettingsRepository.lowAlertLevel.value)
        w.endObject()
        w.endObject()
    }

    private fun levelHistory(w: JsonWriter, samples: List<LevelSample>) {
        w.name("levelHistory").beginObject()
        w.name("count").value(samples.size)
        w.field("firstSampleMs", samples.firstOrNull()?.timeMs)
        w.name("samples").beginArray()
        samples.forEach {
            w.beginObject()
            w.name("t").value(it.timeMs)
            w.name("level").value(it.level)
            w.name("plugged").value(it.plugged)
            w.name("screenOn").value(it.screenOn)
            if (it.currentMa != null) w.name("currentMa").value(it.currentMa.toLong())
            if (it.voltageMv != null) w.name("voltageMv").value(it.voltageMv.toLong())
            if (it.tempC != null) w.name("tempC").value(it.tempC.toDouble())
            w.endObject()
        }
        w.endArray()
        w.endObject()
    }

    private fun sessions(w: JsonWriter) {
        val all = SessionRepository.sessions.value.toMutableList()
        SessionRepository.active.value?.let { active -> if (all.none { it.startMs == active.startMs }) all += active }
        w.name("chargeSessions").beginArray()
        all.sortedBy { it.startMs }.forEach { c ->
            w.beginObject()
            w.name("startMs").value(c.startMs)
            w.name("lastMs").value(c.lastMs)
            w.name("durationMs").value(c.durationMs)
            w.name("startLevel").value(c.startLevel)
            w.name("endLevel").value(c.endLevel)
            w.name("peakCurrentMa").value(c.peakCurrentMa)
            w.name("avgCurrentMa").value(c.avgCurrentMa)
            w.name("peakPowerMw").value(c.peakPowerMw)
            w.name("maxTempC").value(c.maxTempC.toDouble())
            w.name("source").value(c.source.name)
            w.name("ongoing").value(c.ongoing)
            w.name("pointFields").beginArray().value("offsetSec").value("level").value("currentMa").endArray()
            w.name("points").beginArray()
            c.points.forEach { p -> w.beginArray().value(p.offsetSec).value(p.level).value(p.currentMa).endArray() }
            w.endArray()
            w.endObject()
        }
        w.endArray()
    }

    private fun drain(w: JsonWriter, samples: List<LevelSample>, nowMs: Long) {
        val d = computeDrainStats(samples, nowMs - 14 * DAY_MS, nowMs)
        w.name("drainLast14Days").beginObject()
        w.field("screenOnPercentPerHour", d.screenOnPph?.toDouble())
        w.field("screenOffPercentPerHour", d.screenOffPph?.toDouble())
        w.name("screenOnHours").value(d.screenOnHours.toDouble())
        w.name("screenOffHours").value(d.screenOffHours.toDouble())
        w.name("byPeriod").beginArray()
        d.byPeriod.forEach {
            w.beginObject()
            w.name("period").value(it.key.name)
            w.name("percentPerHour").value(it.percentPerHour.toDouble())
            w.name("hours").value(it.hours.toDouble())
            w.endObject()
        }
        w.endArray()
        w.endObject()
    }

    private fun apps(w: JsonWriter, context: Context, input: ExportInput, samples: List<LevelSample>, nowMs: Long) {
        w.name("apps").beginObject()
        w.name("usageAccessGranted").value(input.usageGranted)
        if (!input.usageGranted) {
            w.endObject()
            return
        }
        val reader = AppUsageReader(context)
        val first = samples.firstOrNull()?.timeMs
        val capacityMah = input.capacityMah ?: input.designCapacityMah
        fun window(name: String, fromMs: Long, toMs: Long) {
            val histFrom = if (first != null) maxOf(fromMs, first) else null
            val hasHistory = histFrom != null && histFrom < toMs
            val partial = hasHistory && histFrom!! > fromMs
            val drained = if (hasHistory) drainedPercent(samples, histFrom!!, toMs) else null
            val screen = if (hasHistory) screenTimes(samples, histFrom!!, toMs) else null
            val usage = if (fromMs < toMs) reader.read(fromMs, toMs) else emptyList()
            // The estimated shares only make sense over the range that has level history.
            val inHistory = if (partial) reader.read(histFrom!!, toMs) else usage
            w.beginObject()
            w.name("name").value(name)
            w.name("fromMs").value(fromMs)
            w.name("toMs").value(toMs)
            w.field("historyFromMs", if (hasHistory) histFrom else null)
            w.name("partialHistory").value(partial)
            w.field("drainedPercent", drained)
            w.field("drainedMah", if (drained != null && capacityMah != null) drained / 100f * capacityMah else null)
            w.field("screenOnMs", screen?.onMs)
            w.field("screenOffMs", screen?.offMs)
            w.field("historyCoveredMs", screen?.coveredMs)
            appList(w, usage, if (hasHistory) inHistory else null, drained, capacityMah, partial)
            w.endObject()
        }
        w.name("windows").beginArray()
        window("last24h", nowMs - DAY_MS, nowMs)
        window("last14d", nowMs - 14 * DAY_MS, nowMs)
        w.endArray()
        w.name("daily").beginArray()
        for (k in 0 until 14) {
            val start = startOfDay(nowMs - k * DAY_MS)
            window("day", start, minOf(start + DAY_MS, nowMs))
        }
        w.endArray()
        w.name("hourlyLast24h").beginArray()
        val lastHour = truncateToHour(nowMs)
        for (k in 0 until 24) {
            val start = lastHour - k * HOUR_MS
            window("hour", start, minOf(start + HOUR_MS, nowMs))
        }
        w.endArray()
        w.endObject()
    }

    private fun appList(
        w: JsonWriter,
        raw: List<RawAppUsage>,
        inHistory: List<RawAppUsage>?,
        drained: Float?,
        capacityMah: Int?,
        partial: Boolean,
    ) {
        fun weight(r: RawAppUsage) = r.foregroundMs + r.backgroundMs * BACKGROUND_WEIGHT
        val counted = inHistory?.filter { it.foregroundMs + it.backgroundMs >= 30_000L }.orEmpty()
        val total = counted.sumOf { weight(it) }
        val countedByPkg = counted.associateBy { it.packageName }
        val historyByPkg = inHistory.orEmpty().associateBy { it.packageName }
        // Apps seen only inside the history range still appear (their usage is at least what the range holds).
        val rawPkgs = raw.map { it.packageName }.toSet()
        val all = raw + inHistory.orEmpty().filter { it.packageName !in rawPkgs }
        w.name("apps").beginArray()
        all.sortedByDescending { weight(it) }.forEach { r ->
            val c = countedByPkg[r.packageName]
            val share = if (total > 0.0 && c != null) weight(c) / total else null
            w.beginObject()
            w.name("package").value(r.packageName)
            w.name("label").value(r.label)
            w.name("system").value(r.isSystem)
            w.name("foregroundMs").value(r.foregroundMs)
            w.name("foregroundServiceMs").value(r.backgroundMs)
            if (partial) {
                val h = historyByPkg[r.packageName]
                w.field("foregroundMsInHistory", h?.foregroundMs ?: 0L)
                w.field("foregroundServiceMsInHistory", h?.backgroundMs ?: 0L)
            }
            w.field("estimatedShare", share)
            w.field("estimatedDrainPercent", if (share != null && drained != null) share * drained else null)
            w.field("estimatedDrainMah", if (share != null && drained != null && capacityMah != null) share * drained / 100f * capacityMah else null)
            w.endObject()
        }
        w.endArray()
    }

    /** Time with the screen on and off between [fromMs] and [toMs], from consecutive level samples (gaps over 2 h are skipped). */
    internal data class ScreenTimes(val onMs: Long, val offMs: Long) {
        val coveredMs: Long get() = onMs + offMs
    }

    internal fun screenTimes(samples: List<LevelSample>, fromMs: Long, toMs: Long): ScreenTimes {
        var on = 0L
        var off = 0L
        samples.zipWithNext().forEach { (a, b) ->
            val start = maxOf(a.timeMs, fromMs)
            val end = minOf(b.timeMs, toMs)
            if (end <= start || b.timeMs - a.timeMs > MAX_GAP_MS) return@forEach
            if (a.screenOn) on += end - start else off += end - start
        }
        return ScreenTimes(on, off)
    }

    private fun JsonWriter.field(key: String, v: Int?) {
        name(key); if (v == null) nullValue() else value(v.toLong())
    }

    private fun JsonWriter.field(key: String, v: Long?) {
        name(key); if (v == null) nullValue() else value(v)
    }

    private fun JsonWriter.field(key: String, v: Float?) {
        name(key); if (v == null) nullValue() else value(v.toDouble())
    }

    private fun JsonWriter.field(key: String, v: Double?) {
        name(key); if (v == null) nullValue() else value(v)
    }

    private fun JsonWriter.field(key: String, v: String?) {
        name(key); if (v == null) nullValue() else value(v)
    }
}
