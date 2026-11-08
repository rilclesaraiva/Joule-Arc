package com.joulearc.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUsageTest {
    private fun raw(pkg: String, fg: Long, bg: Long = 0L) = RawAppUsage(pkg, pkg, fg, bg)

    @Test fun ranksByWeightedShare() {
        val ranked = rankAppUsage(listOf(raw("a", 600_000), raw("b", 0, 2_400_000), raw("c", 300_000)))
        // weights: a=600k, b=1_200k, c=300k
        assertEquals(listOf("b", "a", "c"), ranked.map { it.packageName })
        assertEquals(1.0, ranked.sumOf { it.share.toDouble() }, 0.001)
        assertEquals(1_200_000.0 / 2_100_000.0, ranked[0].share.toDouble(), 0.001)
    }

    @Test fun dropsTinyUsage() {
        val ranked = rankAppUsage(listOf(raw("a", 10_000), raw("b", 120_000)))
        assertEquals(listOf("b"), ranked.map { it.packageName })
    }

    @Test fun emptyInputGivesEmptyList() {
        assertTrue(rankAppUsage(emptyList()).isEmpty())
    }

    @Test fun respectsLimit() {
        val many = (1..50).map { raw("p$it", 60_000L * it) }
        assertEquals(30, rankAppUsage(many).size)
    }
}
