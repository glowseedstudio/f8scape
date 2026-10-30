package com.f8scape.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun testIsNewerVersion() {
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(AppUpdateChecker.isNewerVersion("v1.1.0", "1.0.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(AppUpdateChecker.isNewerVersion("1.0.0.1", "1.0.0"))

        assertFalse(AppUpdateChecker.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("v1.0.0", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("0.9.9", "1.0.0"))
        assertFalse(AppUpdateChecker.isNewerVersion("1.0.0", "1.0.1"))
    }
}
