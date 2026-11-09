package com.joulearc.app.data.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {
    @Test fun parsesWithAndWithoutV() {
        assertEquals(listOf(0, 55, 0), parseVersion("v0.55.0"))
        assertEquals(listOf(1, 2, 3), parseVersion("1.2.3"))
    }

    @Test fun rejectsNonNumbers() {
        assertNull(parseVersion("nightly"))
        assertNull(parseVersion("1.x.0"))
        assertNull(parseVersion(""))
    }

    @Test fun comparesNumberByNumber() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
        assertTrue(isNewerVersion("v1.0.0", "0.9.9"))
        assertFalse(isNewerVersion("0.9.0", "0.10.0"))
    }

    @Test fun equalIsNotNewer() {
        assertFalse(isNewerVersion("v0.1.0", "0.1.0"))
        assertFalse(isNewerVersion("0.1", "0.1.0"))
    }

    @Test fun unreadableVersionIsNeverNewer() {
        assertFalse(isNewerVersion("beta", "0.1.0"))
        assertFalse(isNewerVersion("0.2.0", "dev"))
    }
}
