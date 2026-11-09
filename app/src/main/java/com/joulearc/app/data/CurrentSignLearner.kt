package com.joulearc.app.data

/**
 * Learns how this device signs `CURRENT_NOW`. Vendors differ: some report discharge as negative,
 * some as positive, some never change the sign. The sign is only trusted after both signs were
 * seen and a discharge on battery showed which one means "out of the battery".
 */
class CurrentSignLearner {
    @Volatile private var dischargeSign = 0
    @Volatile private var seenPositive = false
    @Volatile private var seenNegative = false

    fun observe(sign: Int, knownDischarging: Boolean, magnitudeMa: Int) {
        if (sign > 0) seenPositive = true
        if (sign < 0) seenNegative = true
        if (knownDischarging && sign != 0 && magnitudeMa >= 20) dischargeSign = sign
    }

    /** True when this reading clearly means current is flowing out of the battery. */
    fun isDischarge(sign: Int, magnitudeMa: Int): Boolean =
        seenPositive && seenNegative && dischargeSign != 0 && sign == dischargeSign && magnitudeMa >= 30
}
