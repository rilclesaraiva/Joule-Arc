package com.joulearc.app.data.updates

/** Reads "v1.2.3" or "1.2.3" as [1, 2, 3]. Returns null when it is not a plain dotted number. */
fun parseVersion(text: String): List<Int>? {
    val parts = text.trim().removePrefix("v").removePrefix("V").split('.')
    if (parts.isEmpty() || parts.size > 4) return null
    val numbers = parts.map { it.toIntOrNull() ?: return null }
    return if (numbers.any { it < 0 }) null else numbers
}

/** True when [remote] is a higher version than [local], compared number by number (0.10.0 > 0.9.0). */
fun isNewerVersion(remote: String, local: String): Boolean {
    val r = parseVersion(remote) ?: return false
    val l = parseVersion(local) ?: return false
    for (i in 0 until maxOf(r.size, l.size)) {
        val a = r.getOrElse(i) { 0 }
        val b = l.getOrElse(i) { 0 }
        if (a != b) return a > b
    }
    return false
}
