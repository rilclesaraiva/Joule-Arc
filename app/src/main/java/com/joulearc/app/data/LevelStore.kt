package com.joulearc.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Reads and writes the battery level history as a compact JSON array:
 * [[time, level, plugged, screen, currentMa, voltageMv, tempDeciC], ...] (the last three can be null or missing).
 */
internal class LevelStore(private val file: File) {

    fun load(): List<LevelSample> =
        try {
            if (!file.exists()) emptyList()
            else {
                val array = JSONArray(file.readText())
                (0 until array.length()).map {
                    val row = array.getJSONArray(it)
                    LevelSample(
                        row.getLong(0), row.getInt(1), row.getInt(2) == 1, row.optInt(3, 1) == 1,
                        currentMa = if (row.isNull(4)) null else row.getInt(4),
                        voltageMv = if (row.isNull(5)) null else row.getInt(5),
                        tempC = if (row.isNull(6)) null else row.getInt(6) / 10f,
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

    fun save(samples: List<LevelSample>) {
        val array = JSONArray()
        samples.forEach { array.put(
            JSONArray().put(it.timeMs).put(it.level).put(if (it.plugged) 1 else 0).put(if (it.screenOn) 1 else 0)
                .put(it.currentMa ?: JSONObject.NULL).put(it.voltageMv ?: JSONObject.NULL)
                .put(it.tempC?.let { t -> Math.round(t * 10f) } ?: JSONObject.NULL),
        ) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
