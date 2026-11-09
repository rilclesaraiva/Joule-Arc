package com.joulearc.app.data

/**
 * Holds back the amber warning. A charger that was just plugged in often looks wrong for a few
 * seconds (handshake, fast-charge negotiation), so a reading counts as a problem only after it has
 * stayed odd, without a break, for [holdMs].
 */
class AmberGate(private val holdMs: Long = DEFAULT_HOLD_MS) {
    private var oddSince: Long? = null

    /** Feed each reading; returns true once the odd state has lasted long enough. */
    fun confirmed(odd: Boolean, nowMs: Long): Boolean {
        if (!odd) {
            oddSince = null
            return false
        }
        val since = oddSince ?: nowMs.also { oddSince = it }
        return nowMs - since >= holdMs
    }

    companion object {
        const val DEFAULT_HOLD_MS = 45_000L
    }
}
