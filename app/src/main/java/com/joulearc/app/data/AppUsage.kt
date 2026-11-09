package com.joulearc.app.data

data class RawAppUsage(
    val packageName: String,
    val label: String,
    val foregroundMs: Long,
    /** Time the app ran a foreground service (the closest public measure of background work). */
    val backgroundMs: Long,
    /** True for system apps and for apps without a launcher icon (hidden by default in the list). */
    val isSystem: Boolean = false,
)

data class AppUsage(
    val packageName: String,
    val label: String,
    val foregroundMs: Long,
    val backgroundMs: Long,
    /** Estimated share of the usage-based drain, 0..1. Not exact mAh. */
    val share: Float,
    val isSystem: Boolean = false,
)

/** Background work counts less than screen-on time when estimating drain. */
const val BACKGROUND_WEIGHT = 0.5

/** Ranks apps by an estimated share of battery use based on usage time. */
fun rankAppUsage(
    raw: List<RawAppUsage>,
    limit: Int = 100,
    minMs: Long = 30_000L,
): List<AppUsage> {
    fun weight(r: RawAppUsage) = r.foregroundMs + r.backgroundMs * BACKGROUND_WEIGHT
    val kept = raw.filter { it.foregroundMs + it.backgroundMs >= minMs }
    val total = kept.sumOf { weight(it) }
    if (total <= 0.0) return emptyList()
    return kept
        .map { AppUsage(it.packageName, it.label, it.foregroundMs, it.backgroundMs, (weight(it) / total).toFloat(), it.isSystem) }
        .sortedByDescending { it.share }
        .take(limit)
}
