package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ClipboardMonitorConfigTest {

    // ── Default Config ──────────────────────────────────────────────

    @Test
    fun defaultConfig_hasExpectedValues() {
        val config = ClipboardMonitorConfig.Default
        assertEquals(1000L, config.pollingIntervalMs)
        assertTrue(config.detectUrls)
        assertTrue(config.showNotification)
        assertFalse(config.showOverlay)
        assertFalse(config.triggerWorkerOnUrl)
    }

    @Test
    fun defaultConfig_hasDefaultNotificationText() {
        val config = ClipboardMonitorConfig.Default
        assertEquals("Clipboard Monitor Service", config.notificationTitle)
        assertEquals("Monitoring clipboard changes", config.notificationText)
    }

    // ── SocialMediaDownloader Config ────────────────────────────────

    @Test
    fun socialMediaConfig_hasExpectedValues() {
        val config = ClipboardMonitorConfig.SocialMediaDownloader
        assertEquals(500L, config.pollingIntervalMs)
        assertTrue(config.detectUrls)
        assertTrue(config.showNotification)
        assertTrue(config.showOverlay)
        assertTrue(config.triggerWorkerOnUrl)
    }

    // ── Custom Config ───────────────────────────────────────────────

    @Test
    fun customConfig_overridesDefaults() {
        val config = ClipboardMonitorConfig(
            pollingIntervalMs = 2000L,
            detectUrls = false,
            showNotification = false,
            showOverlay = true,
            triggerWorkerOnUrl = true,
            notificationTitle = "Custom Title",
            notificationText = "Custom Text",
        )
        assertEquals(2000L, config.pollingIntervalMs)
        assertFalse(config.detectUrls)
        assertFalse(config.showNotification)
        assertTrue(config.showOverlay)
        assertTrue(config.triggerWorkerOnUrl)
        assertEquals("Custom Title", config.notificationTitle)
        assertEquals("Custom Text", config.notificationText)
    }

    // ── Monitor State ───────────────────────────────────────────────

    @Test
    fun monitorState_idle() {
        val state: ClipboardMonitorState = ClipboardMonitorState.Idle
        assertIs<ClipboardMonitorState.Idle>(state)
    }

    @Test
    fun monitorState_monitoring() {
        val state: ClipboardMonitorState = ClipboardMonitorState.Monitoring
        assertIs<ClipboardMonitorState.Monitoring>(state)
    }

    @Test
    fun monitorState_paused() {
        val state: ClipboardMonitorState = ClipboardMonitorState.Paused
        assertIs<ClipboardMonitorState.Paused>(state)
    }

    @Test
    fun monitorState_error() {
        val state = ClipboardMonitorState.Error("Something went wrong", RuntimeException("test"))
        assertIs<ClipboardMonitorState.Error>(state)
        assertEquals("Something went wrong", state.message)
        assertTrue(state.cause is RuntimeException)
    }

    @Test
    fun monitorState_errorWithoutCause() {
        val state = ClipboardMonitorState.Error("No cause")
        assertEquals(null, state.cause)
    }
}
