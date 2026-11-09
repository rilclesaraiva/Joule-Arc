package com.joulearc.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory latest reading and estimates, written by the monitor service and read by the UI. */
object BatteryRepository {
    private val _snapshot = MutableStateFlow<BatterySnapshot?>(null)
    val snapshot: StateFlow<BatterySnapshot?> = _snapshot.asStateFlow()

    private val _estimate = MutableStateFlow<ChargeEstimate?>(null)
    val estimate: StateFlow<ChargeEstimate?> = _estimate.asStateFlow()

    private val _capacityMah = MutableStateFlow<Int?>(null)
    val capacityMah: StateFlow<Int?> = _capacityMah.asStateFlow()

    private val _extremes = MutableStateFlow(ReadingExtremes())
    /** Lowest and highest readings since the app was opened. */
    val extremes: StateFlow<ReadingExtremes> = _extremes.asStateFlow()

    /** True while the app screen is visible: the monitor samples faster. */
    private val _uiVisible = MutableStateFlow(false)
    val uiVisible: StateFlow<Boolean> = _uiVisible.asStateFlow()

    fun publish(value: BatterySnapshot) {
        _snapshot.value = value
        _extremes.value = _extremes.value.update(value)
    }

    fun publishEstimate(value: ChargeEstimate?) {
        _estimate.value = value
    }

    fun publishCapacity(value: Int?) {
        _capacityMah.value = value
    }

    fun resetExtremes() {
        _extremes.value = _snapshot.value?.let { ReadingExtremes().update(it) } ?: ReadingExtremes()
    }

    fun setUiVisible(visible: Boolean) {
        _uiVisible.value = visible
    }
}
