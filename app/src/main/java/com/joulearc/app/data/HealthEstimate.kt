package com.joulearc.app.data

/** Health as estimated capacity over rated capacity, capped at 100%. Null without both values. */
fun estimateHealthPercent(estimatedMah: Int?, designMah: Int?): Int? {
    if (estimatedMah == null || designMah == null || designMah <= 0) return null
    return (estimatedMah * 100f / designMah).toInt().coerceIn(1, 100)
}
