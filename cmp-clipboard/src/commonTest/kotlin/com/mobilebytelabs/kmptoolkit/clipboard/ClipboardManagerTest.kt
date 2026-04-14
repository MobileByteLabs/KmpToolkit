package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClipboardManagerTest {

    // ── Creation ────────────────────────────────────────────────

    @Test
    fun create_withDefaults() {
        val manager = ClipboardManager()
        assertNotNull(manager)
        assertFalse(manager.isActive)
    }

    @Test
    fun create_withFullConfig() {
        val manager = ClipboardManager(ClipboardManagerConfig.Full)
        assertNotNull(manager)
    }

    @Test
    fun create_withSocialMediaConfig() {
        val manager = ClipboardManager(ClipboardManagerConfig.SocialMediaDownloader)
        assertNotNull(manager)
    }

    @Test
    fun create_withCustomConfig() {
        val manager = ClipboardManager(ClipboardManagerConfig(
            observe = true,
            historySize = 50,
            detectUrls = true,
            urlMatchers = SocialMediaUrlMatchers.all(),
            filters = listOf(ClipboardFilter.urlOnly()),
            async = true,
        ))
        assertEquals(50, manager.historyMaxSize)
    }

    // ── Config Presets ──────────────────────────────────────────

    @Test
    fun defaultConfig_values() {
        val config = ClipboardManagerConfig.Default
        assertTrue(config.observe)
        assertEquals(20, config.historySize)
        assertFalse(config.detectUrls)
        assertFalse(config.showNotification)
    }

    @Test
    fun fullConfig_values() {
        val config = ClipboardManagerConfig.Full
        assertTrue(config.observe)
        assertEquals(30, config.historySize)
        assertTrue(config.detectUrls)
        assertTrue(config.async)
        assertEquals(10, config.urlMatchers.size)
    }

    @Test
    fun socialMediaConfig_values() {
        val config = ClipboardManagerConfig.SocialMediaDownloader
        assertTrue(config.detectUrls)
        assertTrue(config.showNotification)
        assertTrue(config.showOverlay)
        assertTrue(config.triggerWorkerOnUrl)
        assertTrue(config.filters.isNotEmpty())
        assertEquals(500L, config.pollingIntervalMs)
    }

    @Test
    fun minimalConfig_values() {
        val config = ClipboardManagerConfig.Minimal
        assertFalse(config.observe)
        assertEquals(0, config.historySize)
        assertFalse(config.detectUrls)
    }

    @Test
    fun chatMonitorConfig_values() {
        val config = ClipboardManagerConfig.ChatMonitor
        assertTrue(config.observe)
        assertEquals(50, config.historySize)
        assertTrue(config.async)
        assertFalse(config.detectUrls)
    }

    @Test
    fun linkCollectorConfig_values() {
        val config = ClipboardManagerConfig.LinkCollector
        assertTrue(config.observe)
        assertEquals(100, config.historySize)
        assertTrue(config.detectUrls)
        assertTrue(config.async)
        assertEquals(10, config.urlMatchers.size)
        assertFalse(config.showNotification)
        assertFalse(config.showOverlay)
    }

    @Test
    fun secureClipboardConfig_values() {
        val config = ClipboardManagerConfig.SecureClipboard
        assertTrue(config.observe)
        assertEquals(0, config.historySize)
        assertEquals(30_000L, config.autoClearAfterReadMs)
        assertFalse(config.detectUrls)
    }

    @Test
    fun developerConfig_values() {
        val config = ClipboardManagerConfig.Developer
        assertTrue(config.observe)
        assertEquals(100, config.historySize)
        assertTrue(config.detectUrls)
        assertTrue(config.async)
        assertEquals(500L, config.pollingIntervalMs)
        assertFalse(config.showNotification)
    }

    @Test
    fun allPresets_canCreateManager() {
        val presets = listOf(
            ClipboardManagerConfig.Minimal,
            ClipboardManagerConfig.Default,
            ClipboardManagerConfig.ChatMonitor,
            ClipboardManagerConfig.LinkCollector,
            ClipboardManagerConfig.SecureClipboard,
            ClipboardManagerConfig.SocialMediaDownloader,
            ClipboardManagerConfig.Developer,
            ClipboardManagerConfig.Full,
        )
        presets.forEach { config ->
            val manager = ClipboardManager(config)
            assertNotNull(manager)
        }
    }

    // ── Sync Operations ─────────────────────────────────────────

    @Test
    fun copy_doesNotThrow() {
        val manager = ClipboardManager()
        manager.copy("test")
    }

    @Test
    fun paste_doesNotThrow() {
        val manager = ClipboardManager()
        manager.paste() // may return null
    }

    @Test
    fun hasText_doesNotThrow() {
        val manager = ClipboardManager()
        manager.hasText()
    }

    @Test
    fun clear_doesNotThrow() {
        val manager = ClipboardManager()
        manager.clear()
    }

    @Test
    fun copyAndPaste_roundTrip() {
        val manager = ClipboardManager()
        val text = "ClipboardManager round-trip test"
        val success = manager.copy(text)
        if (success && manager.hasText()) {
            val result = manager.paste()
            result?.let { assertTrue(it.contains("ClipboardManager")) }
        }
    }

    // ── Lifecycle ───────────────────────────────────────────────

    @Test
    fun start_setsActive() {
        val manager = ClipboardManager()
        manager.start()
        assertTrue(manager.isActive)
        manager.stop()
    }

    @Test
    fun stop_setsInactive() {
        val manager = ClipboardManager()
        manager.start()
        manager.stop()
        assertFalse(manager.isActive)
    }

    @Test
    fun doubleStart_isIdempotent() {
        val manager = ClipboardManager()
        manager.start()
        manager.start()
        assertTrue(manager.isActive)
        manager.stop()
    }

    @Test
    fun doubleStop_isIdempotent() {
        val manager = ClipboardManager()
        manager.start()
        manager.stop()
        manager.stop()
        assertFalse(manager.isActive)
    }

    @Test
    fun pauseResume_doesNotThrow() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        manager.start()
        manager.pause()
        manager.resume()
        manager.stop()
    }

    // ── Flows Accessible ────────────────────────────────────────

    @Test
    fun content_isAccessible() {
        val manager = ClipboardManager()
        assertNotNull(manager.content)
    }

    @Test
    fun history_isAccessible() {
        val manager = ClipboardManager()
        assertNotNull(manager.history)
        assertTrue(manager.history.value.isEmpty())
    }

    @Test
    fun urlDetections_isAccessible() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        assertNotNull(manager.urlDetections)
    }

    @Test
    fun changes_isAccessible() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        assertNotNull(manager.changes)
    }

    // ── History Operations ──────────────────────────────────────

    @Test
    fun clearHistory_doesNotThrow() {
        val manager = ClipboardManager()
        manager.clearHistory()
    }

    @Test
    fun removeFromHistory_doesNotThrow() {
        val manager = ClipboardManager()
        val entry = ClipboardHistoryEntry("test", 1000L)
        manager.removeFromHistory(entry)
    }

    // ── URL Matchers ────────────────────────────────────────────

    @Test
    fun addUrlMatcher_doesNotThrow() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        manager.addUrlMatcher(SocialMediaUrlMatchers.instagram())
    }

    @Test
    fun addAllSocialMediaMatchers_doesNotThrow() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        manager.addAllSocialMediaMatchers()
    }

    @Test
    fun addFilter_doesNotThrow() {
        val manager = ClipboardManager(ClipboardManagerConfig(detectUrls = true))
        manager.addFilter(ClipboardFilter.urlOnly())
    }

    // ── Permissions ─────────────────────────────────────────────

    @Test
    fun hasClipboardAccess_doesNotThrow() {
        val manager = ClipboardManager()
        manager.hasClipboardAccess()
    }

    @Test
    fun hasOverlayPermission_doesNotThrow() {
        val manager = ClipboardManager()
        manager.hasOverlayPermission()
    }

    // ── Full Lifecycle ──────────────────────────────────────────

    @Test
    fun fullLifecycle() {
        val manager = ClipboardManager(ClipboardManagerConfig.Full)

        manager.start()
        assertTrue(manager.isActive)

        manager.copy("Hello from ClipboardManager")
        manager.paste()
        manager.hasText()

        manager.pause()
        manager.resume()

        manager.clearHistory()
        manager.stop()
        assertFalse(manager.isActive)
    }
}
