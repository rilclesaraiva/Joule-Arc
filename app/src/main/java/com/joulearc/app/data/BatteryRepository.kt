package com.joulearc.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory latest reading and estimate, written by the monitor service and read by the UI. */
object BatteryRepository {
    private val _snapshot = MutableStateFlow<BatterySnapshot?>(null)
    val snapshot: StateFlow<BatterySnapshot?> = _snapshot.asStateFlow()

    private val _estimate = MutableStateFlow<ChargeEstimate?>(null)
    val estimate: StateFlow<ChargeEstimate?> = _estimate.asStateFlow()

    /** True while the app screen is visible: the monitor samples faster. */
    private val _uiVisible = MutableStateFlow(false)
    val uiVisible: StateFlow<Boolean> = _uiVisible.asStateFlow()

    fun publish(value: BatterySnapshot) {
        _snapshot.value = value
    }

    fun publishEstimate(value: ChargeEstimate?) {
        _estimate.value = value
    }

    fun setUiVisible(visible: Boolean) {
        _uiVisible.value = visible
    }
}
