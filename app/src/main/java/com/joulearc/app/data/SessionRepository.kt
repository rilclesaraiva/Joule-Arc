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

/** Charge session history: finished sessions (newest first) plus the one in progress. */
object SessionRepository {
    private const val MAX_SESSIONS = 200
    private const val MIN_DURATION_MS = 60_000L
    private const val SAVE_INTERVAL_MS = 30_000L
    /** Only the newest sessions keep their curve, to keep the file small. */
    private const val MAX_SESSIONS_WITH_CURVE = 30

    private val mutex = Mutex()
    private var store: SessionStore? = null
    private var tracker = ChargeSessionTracker()
    private var lastSavedMs = 0L

    private val _sessions = MutableStateFlow<List<ChargeSession>>(emptyList())
    val sessions: StateFlow<List<ChargeSession>> = _sessions.asStateFlow()

    private val _active = MutableStateFlow<ChargeSession?>(null)
    val active: StateFlow<ChargeSession?> = _active.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (store != null) return
        val s = SessionStore(File(context.applicationContext.filesDir, "sessions.json"))
        val state = s.load()
        _sessions.value = state.sessions
        tracker = ChargeSessionTracker(state.active)
        _active.value = state.active
        store = s
    }

    suspend fun onSnapshot(snapshot: BatterySnapshot) {
        mutex.withLock {
            val s = store ?: return
            val wasPending = tracker.pending != null
            val closed = tracker.onSnapshot(snapshot)
            var mustSave = (tracker.pending != null) != wasPending
            if (closed != null) {
                if (closed.durationMs >= MIN_DURATION_MS) {
                    _sessions.value = (listOf(closed) + _sessions.value)
                        .take(MAX_SESSIONS)
                        .mapIndexed { i, session ->
                            if (i >= MAX_SESSIONS_WITH_CURVE && session.points.isNotEmpty()) {
                                session.copy(points = emptyList())
                            } else session
                        }
                }
                mustSave = true
            }
            val activeNow = tracker.current
            // While waiting for a reconnection the session still shows, marked as paused.
            _active.value = activeNow ?: tracker.pending
            // A session waiting for a possible reconnection is saved too, so it survives the app dying.
            val toSave = activeNow ?: tracker.pending
            if (mustSave || (toSave != null && snapshot.timestampMs - lastSavedMs >= SAVE_INTERVAL_MS)) {
                val state = SessionStore.State(_sessions.value, toSave)
                withContext(Dispatchers.IO) { s.save(state) }
                lastSavedMs = snapshot.timestampMs
            }
        }
    }
}
