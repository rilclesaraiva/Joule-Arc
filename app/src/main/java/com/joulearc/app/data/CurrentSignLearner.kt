package com.joulearc.app.data

/**
 * Learns how this device signs `CURRENT_NOW`. Vendors differ: some report discharge as negative,
 * some as positive, some never change the sign.
 *
 * It keeps votes instead of trusting one reading, because the first reading after plugging or
 * unplugging can still carry the old sign. A discharge on battery votes for its own sign; a steady
 * charge (charger connected, status CHARGING, a good current) votes for the opposite one. The sign
 * is only used once both signs were seen and one side clearly leads.
 */
class CurrentSignLearner {
    @Volatile private var negativeIsDischarge = 0
    @Volatile private var positiveIsDischarge = 0
    @Volatile private var seenPositive = false
    @Volatile private var seenNegative = false

    fun observe(sign: Int, knownDischarging: Boolean, magnitudeMa: Int, knownCharging: Boolean = false) {
        if (sign > 0) seenPositive = true
        if (sign < 0) seenNegative = true
        if (sign == 0) return
        if (knownDischarging && magnitudeMa >= MIN_DISCHARGE_MA) vote(discharge = sign)
        if (knownCharging && magnitudeMa >= MIN_CHARGE_MA) vote(discharge = -sign)
    }

    private fun vote(discharge: Int) {
        if (discharge < 0) negativeIsDischarge++ else positiveIsDischarge++
        if (negativeIsDischarge + positiveIsDischarge > MAX_VOTES) {
            negativeIsDischarge /= 2
            positiveIsDischarge /= 2
        }
    }

    /** The sign that means "out of the battery", or 0 while it is not clear yet. */
    private fun dischargeSign(): Int {
        if (!(seenPositive && seenNegative)) return 0
        val neg = negativeIsDischarge
        val pos = positiveIsDischarge
        return when {
            neg >= MIN_VOTES && neg >= 2 * pos -> -1
            pos >= MIN_VOTES && pos >= 2 * neg -> 1
            else -> 0
        }
    }

    /** True when this reading clearly means current is flowing out of the battery. */
    fun isDischarge(sign: Int, magnitudeMa: Int): Boolean {
        val d = dischargeSign()
        return d != 0 && sign == d && magnitudeMa >= 30
    }

    /** True when this reading clearly means current is flowing into the battery. */
    fun isCharge(sign: Int, magnitudeMa: Int): Boolean {
        val d = dischargeSign()
        return d != 0 && sign == -d && magnitudeMa >= 50
    }

    private companion object {
        const val MIN_VOTES = 3
        const val MAX_VOTES = 2000
        const val MIN_DISCHARGE_MA = 20
        const val MIN_CHARGE_MA = 300
    }
}
