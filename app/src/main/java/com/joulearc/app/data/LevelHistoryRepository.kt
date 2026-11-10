package com.joulearc.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Battery level history of the last 14 days, kept in the app's private storage. */
object LevelHistoryRepository {
    private const val RETENTION_MS = 14L * 24 * 3_600_000
    private const val MIN_INTERVAL_MS = 10L * 60_000
    private const val SAVE_INTERVAL_MS = 5L * 60_000

    private val mutex = Mutex()
    private var store: LevelStore? = null
    private var lastSavedMs = 0L

    private val _samples = MutableStateFlow<List<LevelSample>>(emptyList())
    val samples: StateFlow<List<LevelSample>> = _samples.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = LevelStore(File(context.applicationContext.filesDir, "levels.json"))
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        _samples.value = s.load().filter { it.timeMs >= cutoff }
        store = s
    }

    suspend fun onSnapshot(snapshot: BatterySnapshot) {
        mutex.withLock {
            val s = store ?: return
            val plugged = snapshot.source != PowerSource.BATTERY
            val list = _samples.value
            val last = list.lastOrNull()
            val pluggedChanged = last != null && last.plugged != plugged
            val screenChanged = last != null && last.screenOn != snapshot.screenOn
            val changed = last == null || pluggedChanged || screenChanged || last.level != snapshot.levelPercent ||
                snapshot.timestampMs - last.timeMs >= MIN_INTERVAL_MS
            if (!changed) return

            val cutoff = snapshot.timestampMs - RETENTION_MS
            val updated = (list + LevelSample(
                snapshot.timestampMs, snapshot.levelPercent, plugged, snapshot.screenOn,
                currentMa = snapshot.signedCurrentMa, voltageMv = snapshot.voltageMv, tempC = snapshot.temperatureC,
            ))
                .dropWhile { it.timeMs < cutoff }
            _samples.value = updated

            if (last == null || pluggedChanged || snapshot.timestampMs - lastSavedMs >= SAVE_INTERVAL_MS) {
                withContext(Dispatchers.IO) { s.save(updated) }
                lastSavedMs = snapshot.timestampMs
            }
        }
    }
}
