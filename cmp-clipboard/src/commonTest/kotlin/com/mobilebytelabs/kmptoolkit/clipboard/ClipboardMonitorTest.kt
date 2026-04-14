package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardMonitorTest {

    // ── Factory ─────────────────────────────────────────────────────

    @Test
    fun createClipboardMonitor_returnsInstance() {
        val monitor = createClipboardMonitor()
        assertTrue(monitor is ClipboardMonitor)
    }

    // ── Initial State ───────────────────────────────────────────────

    @Test
    fun monitor_initialStateIsIdle() {
        val monitor = createClipboardMonitor()
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    @Test
    fun monitor_initialLatestChangeIsNull() {
        val monitor = createClipboardMonitor()
        assertNull(monitor.latestChange.value)
    }

    // ── Start / Stop Lifecycle ──────────────────────────────────────

    @Test
    fun monitor_startTransitionsToMonitoring() {
        val monitor = createClipboardMonitor()
        monitor.start()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_stopTransitionsToIdle() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.stop()
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    @Test
    fun monitor_doubleStartIsIdempotent() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.start() // should not throw
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_doubleStopIsIdempotent() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.stop()
        monitor.stop() // should not throw
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    @Test
    fun monitor_stopWithoutStartIsIdempotent() {
        val monitor = createClipboardMonitor()
        monitor.stop() // should not throw
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    // ── Pause / Resume ──────────────────────────────────────────────

    @Test
    fun monitor_pauseTransitionsToPaused() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.pause()
        assertIs<ClipboardMonitorState.Paused>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_resumeTransitionsBackToMonitoring() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.pause()
        monitor.resume()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_pauseWhileIdleDoesNothing() {
        val monitor = createClipboardMonitor()
        monitor.pause() // not started, should be no-op
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    @Test
    fun monitor_resumeWhileIdleDoesNothing() {
        val monitor = createClipboardMonitor()
        monitor.resume() // not paused, should be no-op
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }

    @Test
    fun monitor_resumeWhileMonitoringDoesNothing() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.resume() // already monitoring, should be no-op
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    // ── Configuration ───────────────────────────────────────────────

    @Test
    fun monitor_startWithCustomConfig() {
        val monitor = createClipboardMonitor()
        val config = ClipboardMonitorConfig(
            pollingIntervalMs = 500L,
            detectUrls = true,
            showNotification = false,
        )
        monitor.start(config)
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_startWithSocialMediaConfig() {
        val monitor = createClipboardMonitor()
        monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    // ── URL Matchers ────────────────────────────────────────────────

    @Test
    fun monitor_addUrlMatcher_doesNotThrow() {
        val monitor = createClipboardMonitor()
        monitor.addUrlMatcher(SocialMediaUrlMatchers.instagram())
        monitor.addUrlMatcher(SocialMediaUrlMatchers.tiktok())
        // Should not throw — matchers are stored for later use
    }

    @Test
    fun monitor_addAllUrlMatchers_doesNotThrow() {
        val monitor = createClipboardMonitor()
        SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
    }

    @Test
    fun monitor_addUrlMatcherBeforeStart() {
        val monitor = createClipboardMonitor()
        monitor.addUrlMatcher(SocialMediaUrlMatchers.instagram())
        monitor.start()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    @Test
    fun monitor_addUrlMatcherAfterStart() {
        val monitor = createClipboardMonitor()
        monitor.start()
        monitor.addUrlMatcher(SocialMediaUrlMatchers.youtube())
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    // ── Filters ─────────────────────────────────────────────────────

    @Test
    fun monitor_addFilter_doesNotThrow() {
        val monitor = createClipboardMonitor()
        monitor.addFilter(ClipboardFilter.urlOnly())
        monitor.addFilter(ClipboardFilter.minLength(10))
    }

    @Test
    fun monitor_addFilterBeforeStart() {
        val monitor = createClipboardMonitor()
        monitor.addFilter(ClipboardFilter.urlOnly())
        monitor.start()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
    }

    // ── Full Lifecycle ──────────────────────────────────────────────

    @Test
    fun monitor_fullLifecycle() {
        val monitor = createClipboardMonitor()

        // Configure
        SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
        monitor.addFilter(ClipboardFilter.urlOnly())

        // Start
        monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)

        // Pause
        monitor.pause()
        assertIs<ClipboardMonitorState.Paused>(monitor.state.value)

        // Resume
        monitor.resume()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)

        // Stop
        monitor.stop()
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }
}
