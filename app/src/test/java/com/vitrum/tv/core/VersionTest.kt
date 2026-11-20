package com.vitrum.tv.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionTest {

    @Test
    fun parsesTagsAndVersionNames() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3-debug"))
        assertNull(SemVer.parse("latest"))
        assertNull(SemVer.parse("v1.2"))
        assertNull(SemVer.parse(""))
    }

    @Test
    fun versionCodeFollowsScheme() {
        assertEquals(10203L, SemVer(1, 2, 3).versionCode)
        assertEquals(10000L, SemVer(1, 0, 0).versionCode)
        assertEquals(20403L, SemVer(2, 4, 3).versionCode)
    }

    @Test
    fun comparesSemver() {
        assertTrue(SemVer(1, 0, 1) > SemVer(1, 0, 0))
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 99, 99))
        assertTrue(SemVer(1, 2, 1) > SemVer(1, 2, 0))
        assertEquals(SemVer(1, 2, 3), SemVer(1, 2, 3))
    }

    @Test
    fun updateAvailableOnlyWhenRemoteIsNewer() {
        assertTrue(VersionLogic.isUpdateAvailable(SemVer(1, 0, 1), "1.0.0"))
        assertFalse(VersionLogic.isUpdateAvailable(SemVer(1, 0, 0), "1.0.0"))
        assertFalse(VersionLogic.isUpdateAvailable(SemVer(0, 9, 0), "1.0.0"))
        assertFalse(VersionLogic.isUpdateAvailable(SemVer(1, 0, 1), "not-a-version"))
    }

    @Test
    fun versionCodeForName() {
        assertEquals(10100, VersionLogic.versionCodeFor("1.1.0"))
        assertEquals(0, VersionLogic.versionCodeFor("beta"))
    }
}
