package com.joulearc.app.data

import org.json.JSONArray
import java.io.File

/** Reads and writes the battery level history as a compact JSON array: [[time, level, plugged, screen], ...]. */
internal class LevelStore(private val file: File) {

    fun load(): List<LevelSample> =
        try {
            if (!file.exists()) emptyList()
            else {
                val array = JSONArray(file.readText())
                (0 until array.length()).map {
                    val row = array.getJSONArray(it)
                    LevelSample(row.getLong(0), row.getInt(1), row.getInt(2) == 1, row.optInt(3, 1) == 1)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }

    fun save(samples: List<LevelSample>) {
        val array = JSONArray()
        samples.forEach { array.put(
            JSONArray().put(it.timeMs).put(it.level).put(if (it.plugged) 1 else 0).put(if (it.screenOn) 1 else 0),
        ) }
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(array.toString())
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
