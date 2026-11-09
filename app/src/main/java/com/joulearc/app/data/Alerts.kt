package com.joulearc.app.data

enum class AlertKind { CHARGE_LIMIT, LOW_BATTERY }

/**
 * Decides when to send the charge and low battery reminders. Each fires once per crossing and is
 * armed again only after unplugging (charge) or plugging in (low), or when the level moves 5
 * points back, so it never repeats on small changes.
 */
class AlertTracker {
    private var chargeArmed = true
    private var lowArmed = true

    fun update(level: Int, plugged: Boolean, chargeAt: Int?, lowAt: Int?): List<AlertKind> {
        val out = ArrayList<AlertKind>(2)
        if (chargeAt != null) {
            if (!plugged || level < chargeAt - 5) {
                chargeArmed = true
            } else if (chargeArmed && level >= chargeAt) {
                out += AlertKind.CHARGE_LIMIT
                chargeArmed = false
            }
        }
        if (lowAt != null) {
            if (plugged || level > lowAt + 5) {
                lowArmed = true
            } else if (lowArmed && level <= lowAt) {
                out += AlertKind.LOW_BATTERY
                lowArmed = false
            }
        }
        return out
    }
}
