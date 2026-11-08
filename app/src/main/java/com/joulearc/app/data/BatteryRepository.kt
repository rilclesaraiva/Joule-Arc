package com.joulearc.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory latest reading, written by the monitor service and read by the UI. */
object BatteryRepository {
    private val _snapshot = MutableStateFlow<BatterySnapshot?>(null)
    val snapshot: StateFlow<BatterySnapshot?> = _snapshot.asStateFlow()

    fun publish(value: BatterySnapshot) {
        _snapshot.value = value
    }
}
