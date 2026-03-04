package com.mobilebytelabs.kmptoolkit.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {

    // ========================================================================
    // Parse Tests
    // ========================================================================

    @Test
    fun parseValidSemver() {
        val version = AppVersion.parse("1.2.3")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertEquals("1.2.3", version.versionName)
    }

    @Test
    fun parseWithVPrefix() {
        val version = AppVersion.parse("v1.2.3")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun parseWithUpperVPrefix() {
        val version = AppVersion.parse("V1.2.3")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun parseTwoParts() {
        val version = AppVersion.parse("1.2")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(0, version.patch)
        assertEquals("1.2.0", version.versionName)
    }

    @Test
    fun parseSinglePart() {
        val version = AppVersion.parse("1")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(0, version.minor)
        assertEquals(0, version.patch)
        assertEquals("1.0.0", version.versionName)
    }

    @Test
    fun parseInvalidReturnsNull() {
        assertNull(AppVersion.parse("invalid"))
        assertNull(AppVersion.parse("a.b.c"))
    }

    @Test
    fun parsePartialInvalidFallsBackToDefaults() {
        // When minor/patch are invalid, they fall back to 0
        val version = AppVersion.parse("1.2.x")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(0, version.patch) // 'x' fails to parse, falls back to 0
    }

    @Test
    fun parseEmptyReturnsNull() {
        assertNull(AppVersion.parse(""))
    }

    @Test
    fun parseWhitespaceTrimmed() {
        val version = AppVersion.parse("  1.2.3  ")
        assertNotNull(version)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun parseWithWhitespaceAndPrefix() {
        val version = AppVersion.parse("  v1.2.3  ")
        assertNotNull(version)
        assertEquals(1, version.major)
    }

    // ========================================================================
    // Comparison Tests
    // ========================================================================

    @Test
    fun compareToMajorDifference() {
        val v1 = AppVersion.of(1, 0, 0)
        val v2 = AppVersion.of(2, 0, 0)
        assertTrue(v1 < v2)
        assertTrue(v2 > v1)
    }

    @Test
    fun compareToMinorDifference() {
        val v1 = AppVersion.of(1, 0, 0)
        val v2 = AppVersion.of(1, 1, 0)
        assertTrue(v1 < v2)
        assertTrue(v2 > v1)
    }

    @Test
    fun compareToPatchDifference() {
        val v1 = AppVersion.of(1, 0, 0)
        val v2 = AppVersion.of(1, 0, 1)
        assertTrue(v1 < v2)
        assertTrue(v2 > v1)
    }

    @Test
    fun compareToEqualVersions() {
        val v1 = AppVersion.of(1, 2, 3)
        val v2 = AppVersion.of(1, 2, 3)
        assertEquals(0, v1.compareTo(v2))
        assertEquals(v1, v2)
    }

    @Test
    fun isOlderThanReturnsTrue() {
        val current = AppVersion.of(1, 0, 0)
        val latest = AppVersion.of(2, 0, 0)
        assertTrue(current.isOlderThan(latest))
    }

    @Test
    fun isOlderThanReturnsFalse() {
        val current = AppVersion.of(2, 0, 0)
        val latest = AppVersion.of(1, 0, 0)
        assertFalse(current.isOlderThan(latest))
    }

    @Test
    fun isNewerThanReturnsTrue() {
        val current = AppVersion.of(2, 0, 0)
        val latest = AppVersion.of(1, 0, 0)
        assertTrue(current.isNewerThan(latest))
    }

    @Test
    fun isNewerThanReturnsFalse() {
        val current = AppVersion.of(1, 0, 0)
        val latest = AppVersion.of(2, 0, 0)
        assertFalse(current.isNewerThan(latest))
    }

    // ========================================================================
    // Factory Tests
    // ========================================================================

    @Test
    fun ofCreatesCorrectVersion() {
        val version = AppVersion.of(1, 2, 3, versionCode = 10)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertEquals("1.2.3", version.versionName)
        assertEquals(10L, version.versionCode)
    }

    @Test
    fun ofWithoutVersionCode() {
        val version = AppVersion.of(1, 2, 3)
        assertNull(version.versionCode)
    }

    @Test
    fun unknownConstant() {
        assertEquals(0, AppVersion.UNKNOWN.major)
        assertEquals(0, AppVersion.UNKNOWN.minor)
        assertEquals(0, AppVersion.UNKNOWN.patch)
        assertEquals("0.0.0", AppVersion.UNKNOWN.versionName)
    }

    @Test
    fun toStringReturnsVersionName() {
        val version = AppVersion.of(1, 2, 3)
        assertEquals("1.2.3", version.toString())
    }
}
