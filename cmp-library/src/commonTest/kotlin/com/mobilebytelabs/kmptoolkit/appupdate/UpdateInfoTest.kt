package com.mobilebytelabs.kmptoolkit.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateInfoTest {

    @Test
    fun isAvailableTrue() {
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = AppVersion.of(1, 0, 0),
            latestVersion = AppVersion.of(2, 0, 0),
        )
        assertTrue(info.isAvailable)
    }

    @Test
    fun isAvailableFalse() {
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = AppVersion.of(1, 0, 0),
        )
        assertFalse(info.isAvailable)
    }

    @Test
    fun currentVersionSet() {
        val currentVersion = AppVersion.of(1, 2, 3)
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = currentVersion,
        )
        assertEquals(currentVersion, info.currentVersion)
    }

    @Test
    fun latestVersionSet() {
        val latestVersion = AppVersion.of(2, 0, 0)
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = AppVersion.of(1, 0, 0),
            latestVersion = latestVersion,
        )
        assertEquals(latestVersion, info.latestVersion)
    }

    @Test
    fun latestVersionNull() {
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = AppVersion.of(1, 0, 0),
        )
        assertNull(info.latestVersion)
    }

    @Test
    fun updateTypeDefault() {
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = AppVersion.of(1, 0, 0),
        )
        assertEquals(UpdateType.NONE, info.updateType)
    }

    @Test
    fun updateTypeSet() {
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = AppVersion.of(1, 0, 0),
            latestVersion = AppVersion.of(2, 0, 0),
            updateType = UpdateType.IMMEDIATE,
        )
        assertEquals(UpdateType.IMMEDIATE, info.updateType)
    }

    @Test
    fun releaseNotesSet() {
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = AppVersion.of(1, 0, 0),
            latestVersion = AppVersion.of(2, 0, 0),
            releaseNotes = "Bug fixes and improvements",
        )
        assertEquals("Bug fixes and improvements", info.releaseNotes)
    }

    @Test
    fun releaseNotesNull() {
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = AppVersion.of(1, 0, 0),
        )
        assertNull(info.releaseNotes)
    }

    @Test
    fun storeUrlSet() {
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = AppVersion.of(1, 0, 0),
            storeUrl = "https://play.google.com/store/apps/details?id=com.example",
        )
        assertEquals("https://play.google.com/store/apps/details?id=com.example", info.storeUrl)
    }

    @Test
    fun storeUrlNull() {
        val info = UpdateInfo(
            isAvailable = false,
            currentVersion = AppVersion.of(1, 0, 0),
        )
        assertNull(info.storeUrl)
    }

    @Test
    fun fullConstructor() {
        val currentVersion = AppVersion.of(1, 0, 0)
        val latestVersion = AppVersion.of(2, 0, 0)
        val info = UpdateInfo(
            isAvailable = true,
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            updateType = UpdateType.FLEXIBLE,
            releaseNotes = "New features",
            storeUrl = "https://example.com",
        )

        assertTrue(info.isAvailable)
        assertEquals(currentVersion, info.currentVersion)
        assertEquals(latestVersion, info.latestVersion)
        assertEquals(UpdateType.FLEXIBLE, info.updateType)
        assertEquals("New features", info.releaseNotes)
        assertEquals("https://example.com", info.storeUrl)
    }
}
