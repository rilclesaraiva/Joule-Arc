package com.joulearc.app.data

enum class UsageEventKind { ACTIVITY_START, ACTIVITY_STOP, SERVICE_START, SERVICE_STOP, SCREEN_OFF, SHUTDOWN }

/**
 * Turns a stream of usage events into per-app foreground time and foreground-service time,
 * clipped to the window that was queried. Intervals still open at [windowEndMs] end there.
 */
class UsageEventAggregator(private val windowEndMs: Long) {
    private val fgStart = HashMap<String, Long>()
    private val serviceCount = HashMap<String, Int>()
    private val serviceStart = HashMap<String, Long>()
    private val fg = HashMap<String, Long>()
    private val bg = HashMap<String, Long>()

    val foregroundMs: Map<String, Long> get() = fg
    val backgroundMs: Map<String, Long> get() = bg

    fun onEvent(pkg: String?, kind: UsageEventKind, timeMs: Long) {
        when (kind) {
            UsageEventKind.ACTIVITY_START -> if (pkg != null && pkg !in fgStart) fgStart[pkg] = timeMs
            UsageEventKind.ACTIVITY_STOP -> if (pkg != null) closeForeground(pkg, timeMs)
            UsageEventKind.SERVICE_START -> if (pkg != null) {
                val n = serviceCount.getOrDefault(pkg, 0)
                if (n == 0) serviceStart[pkg] = timeMs
                serviceCount[pkg] = n + 1
            }
            UsageEventKind.SERVICE_STOP -> if (pkg != null) {
                val n = serviceCount.getOrDefault(pkg, 0)
                if (n > 0) {
                    serviceCount[pkg] = n - 1
                    if (n == 1) closeService(pkg, timeMs)
                }
            }
            UsageEventKind.SCREEN_OFF -> closeAllForeground(timeMs)
            UsageEventKind.SHUTDOWN -> {
                closeAllForeground(timeMs)
                serviceStart.keys.toList().forEach { closeService(it, timeMs) }
                serviceCount.clear()
            }
        }
    }

    /** Closes intervals still open at the end of the window. */
    fun finish() {
        closeAllForeground(windowEndMs)
        serviceStart.keys.toList().forEach { closeService(it, windowEndMs) }
        serviceCount.clear()
    }

    private fun closeForeground(pkg: String, timeMs: Long) {
        val start = fgStart.remove(pkg) ?: return
        fg.merge(pkg, (timeMs - start).coerceAtLeast(0L), Long::plus)
    }

    private fun closeAllForeground(timeMs: Long) {
        fgStart.keys.toList().forEach { closeForeground(it, timeMs) }
    }

    private fun closeService(pkg: String, timeMs: Long) {
        val start = serviceStart.remove(pkg) ?: return
        bg.merge(pkg, (timeMs - start).coerceAtLeast(0L), Long::plus)
    }
}
