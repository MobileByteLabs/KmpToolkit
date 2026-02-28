package com.mobilebytelabs.kmptoolkit.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VersionInfoTest {

    @Test
    fun versionSet() {
        val info = VersionInfo(version = "1.2.3")
        assertEquals("1.2.3", info.version)
    }

    @Test
    fun updateTypeDefault() {
        val info = VersionInfo(version = "1.0.0")
        assertEquals(UpdateType.FLEXIBLE, info.updateType)
    }

    @Test
    fun updateTypeSet() {
        val info = VersionInfo(version = "1.0.0", updateType = UpdateType.IMMEDIATE)
        assertEquals(UpdateType.IMMEDIATE, info.updateType)
    }

    @Test
    fun releaseNotesNull() {
        val info = VersionInfo(version = "1.0.0")
        assertNull(info.releaseNotes)
    }

    @Test
    fun releaseNotesSet() {
        val info = VersionInfo(version = "1.0.0", releaseNotes = "Bug fixes")
        assertEquals("Bug fixes", info.releaseNotes)
    }

    @Test
    fun downloadUrlNull() {
        val info = VersionInfo(version = "1.0.0")
        assertNull(info.downloadUrl)
    }

    @Test
    fun downloadUrlSet() {
        val info = VersionInfo(version = "1.0.0", downloadUrl = "https://example.com/app.apk")
        assertEquals("https://example.com/app.apk", info.downloadUrl)
    }

    @Test
    fun storeUrlNull() {
        val info = VersionInfo(version = "1.0.0")
        assertNull(info.storeUrl)
    }

    @Test
    fun storeUrlSet() {
        val info = VersionInfo(version = "1.0.0", storeUrl = "https://play.google.com")
        assertEquals("https://play.google.com", info.storeUrl)
    }

    @Test
    fun metadataEmpty() {
        val info = VersionInfo(version = "1.0.0")
        assertTrue(info.metadata.isEmpty())
    }

    @Test
    fun metadataSet() {
        val info = VersionInfo(
            version = "1.0.0",
            metadata = mapOf("key1" to "value1", "key2" to "value2"),
        )
        assertEquals(2, info.metadata.size)
        assertEquals("value1", info.metadata["key1"])
        assertEquals("value2", info.metadata["key2"])
    }

    @Test
    fun fullConstructor() {
        val info = VersionInfo(
            version = "2.0.0",
            updateType = UpdateType.IMMEDIATE,
            releaseNotes = "Major update",
            downloadUrl = "https://example.com/app-2.0.0.apk",
            storeUrl = "https://play.google.com/store/apps/details?id=com.example",
            metadata = mapOf("minSdk" to "21"),
        )

        assertEquals("2.0.0", info.version)
        assertEquals(UpdateType.IMMEDIATE, info.updateType)
        assertEquals("Major update", info.releaseNotes)
        assertEquals("https://example.com/app-2.0.0.apk", info.downloadUrl)
        assertEquals("https://play.google.com/store/apps/details?id=com.example", info.storeUrl)
        assertEquals("21", info.metadata["minSdk"])
    }
}
