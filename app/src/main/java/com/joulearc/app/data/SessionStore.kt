package com.joulearc.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Reads and writes the session history as a small JSON file in the app's private storage. */
internal class SessionStore(private val file: File) {

    data class State(val sessions: List<ChargeSession>, val active: ChargeSession?)

    fun load(): State {
        return try {
            if (!file.exists()) return State(emptyList(), null)
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("sessions") ?: JSONArray()
            val sessions = (0 until array.length()).map { decode(array.getJSONObject(it)) }
            val active = root.optJSONObject("active")?.let { decode(it) }
            State(sessions, active)
        } catch (e: Exception) {
            State(emptyList(), null)
        }
    }

    fun save(state: State) {
        val root = JSONObject()
        root.put("sessions", JSONArray().also { arr -> state.sessions.forEach { arr.put(encode(it)) } })
        state.active?.let { root.put("active", encode(it)) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(root.toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    private fun encode(s: ChargeSession) = JSONObject()
        .put("start", s.startMs)
        .put("last", s.lastMs)
        .put("startLevel", s.startLevel)
        .put("endLevel", s.endLevel)
        .put("peakCurrent", s.peakCurrentMa)
        .put("avgCurrent", s.avgCurrentMa)
        .put("peakPower", s.peakPowerMw)
        .put("maxTemp", s.maxTempC.toDouble())
        .put("source", s.source.name)
        .put("ongoing", s.ongoing)
        .put(
            "points",
            JSONArray().also { arr ->
                s.points.forEach { arr.put(JSONArray().put(it.offsetSec).put(it.level).put(it.currentMa)) }
            },
        )

    private fun decode(o: JSONObject) = ChargeSession(
        startMs = o.getLong("start"),
        lastMs = o.getLong("last"),
        startLevel = o.getInt("startLevel"),
        endLevel = o.getInt("endLevel"),
        peakCurrentMa = o.getInt("peakCurrent"),
        avgCurrentMa = o.getDouble("avgCurrent"),
        peakPowerMw = o.getInt("peakPower"),
        maxTempC = o.getDouble("maxTemp").toFloat(),
        source = runCatching { PowerSource.valueOf(o.getString("source")) }.getOrDefault(PowerSource.USB),
        ongoing = o.optBoolean("ongoing", false),
        points = o.optJSONArray("points")?.let { a ->
            (0 until a.length()).map {
                val r = a.getJSONArray(it)
                SessionPoint(r.getInt(0), r.getInt(1), r.getInt(2))
            }
        } ?: emptyList(),
    )
}
