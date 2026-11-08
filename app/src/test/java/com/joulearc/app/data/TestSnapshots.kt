package com.joulearc.app.data

fun snap(
    t: Long,
    status: ChargeStatus = ChargeStatus.CHARGING,
    source: PowerSource = PowerSource.USB,
    level: Int = 50,
    current: Int? = 2000,
    voltageMv: Int = 4000,
    counterUah: Long? = null,
    temp: Float = 30f,
) = BatterySnapshot(
    levelPercent = level,
    currentMa = current,
    voltageMv = voltageMv,
    temperatureC = temp,
    status = status,
    source = source,
    chargeCounterUah = counterUah,
    timestampMs = t,
)
