package com.joulearc.app.data

/** Lowest and highest readings since the app was opened. */
data class ReadingExtremes(
    val minVoltageMv: Int? = null,
    val maxVoltageMv: Int? = null,
    val minTempC: Float? = null,
    val maxTempC: Float? = null,
    val maxChargeMw: Int? = null,
    val maxDrainMw: Int? = null,
) {
    fun update(s: BatterySnapshot): ReadingExtremes {
        val v = s.voltageMv.takeIf { it > 0 }
        val p = s.powerMw
        return ReadingExtremes(
            minVoltageMv = if (v == null) minVoltageMv else minOf(minVoltageMv ?: v, v),
            maxVoltageMv = if (v == null) maxVoltageMv else maxOf(maxVoltageMv ?: v, v),
            minTempC = minOf(minTempC ?: s.temperatureC, s.temperatureC),
            maxTempC = maxOf(maxTempC ?: s.temperatureC, s.temperatureC),
            maxChargeMw = if (s.isCharging && p != null) maxOf(maxChargeMw ?: p, p) else maxChargeMw,
            maxDrainMw = if (s.drawingFromBattery && p != null) maxOf(maxDrainMw ?: p, p) else maxDrainMw,
        )
    }
}
