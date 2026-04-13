package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.RegexUrlMatcher
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.ClipboardOverlayConfig
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.OverlayPosition
import com.mobilebytelabs.kmptoolkit.clipboard.worker.ClipboardWorkerTrigger
import com.mobilebytelabs.kmptoolkit.clipboard.worker.createClipboardWorkerTrigger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the full clipboard monitor pipeline:
 * Monitor → Filter → URL Matcher → Change Emission → Worker Trigger
 */
class ClipboardMonitorIntegrationTest {

    // ── E2E: Monitor + Matchers + Filters ───────────────────────────

    @Test
    fun e2e_monitorWithMatchersAndFilters() {
        val monitor = createClipboardMonitor()

        // Add all social media matchers
        SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }

        // Add URL-only filter (skip plain text)
        monitor.addFilter(ClipboardFilter.urlOnly())
        monitor.addFilter(ClipboardFilter.minLength(10))

        // Start with social media config
        monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)

        // Verify flows are accessible
        assertNotNull(monitor.changes)
        assertNotNull(monitor.urlDetections)
        assertNotNull(monitor.latestChange)
        assertNull(monitor.latestChange.value) // No changes yet

        monitor.stop()
    }

    // ── URL Matcher Integration ─────────────────────────────────────

    @Test
    fun urlMatcher_instagramUrlIsDetected() {
        val matchers = SocialMediaUrlMatchers.all()
        val content = "https://www.instagram.com/reel/ABC123/"

        val matched = matchers.filter { it.matches(content) }
        assertTrue(matched.isNotEmpty())
        assertEquals("Instagram", matched.first().name)

        val url = matched.first().extractUrl(content)
        assertNotNull(url)
        assertTrue(url.contains("instagram.com"))
    }

    @Test
    fun urlMatcher_tiktokUrlIsDetected() {
        val matchers = SocialMediaUrlMatchers.all()
        val content = "Check this out! https://vm.tiktok.com/ZMF2abc/ so funny"

        val matched = matchers.filter { it.matches(content) }
        assertTrue(matched.isNotEmpty())
        assertEquals("TikTok", matched.first().name)
    }

    @Test
    fun urlMatcher_youtubeUrlIsDetected() {
        val matchers = SocialMediaUrlMatchers.all()
        val content = "https://youtu.be/dQw4w9WgXcQ"

        val matched = matchers.filter { it.matches(content) }
        assertTrue(matched.isNotEmpty())
        assertEquals("YouTube", matched.first().name)
    }

    @Test
    fun urlMatcher_plainTextNotDetected() {
        val matchers = SocialMediaUrlMatchers.all()
        val content = "Hello, this is just regular text with no URLs at all"

        val matched = matchers.filter { it.matches(content) }
        assertTrue(matched.isEmpty())
    }

    // ── Custom URL Matcher ──────────────────────────────────────────

    @Test
    fun customMatcher_worksWithMonitor() {
        val customMatcher = RegexUrlMatcher(
            name = "MyService",
            patterns = listOf(Regex("https?://myservice\\.com/video/\\d+"))
        )

        assertTrue(customMatcher.matches("https://myservice.com/video/12345"))
        assertFalse(customMatcher.matches("https://other.com/video/12345"))

        val url = customMatcher.extractUrl("Watch: https://myservice.com/video/12345 cool!")
        assertNotNull(url)
        assertEquals("https://myservice.com/video/12345", url)

        // Can add to monitor
        val monitor = createClipboardMonitor()
        monitor.addUrlMatcher(customMatcher)
        monitor.start()
        monitor.stop()
    }

    // ── Filter + Matcher Pipeline ───────────────────────────────────

    @Test
    fun filterPipeline_urlOnlyWithMatcher() {
        val filter = ClipboardFilter.urlOnly()
        val matcher = SocialMediaUrlMatchers.instagram()

        // URL passes filter AND matches
        val url = "https://www.instagram.com/reel/ABC123/"
        assertTrue(filter.shouldProcess(url))
        assertTrue(matcher.matches(url))

        // Plain text fails filter (matcher never runs)
        val text = "just some text"
        assertFalse(filter.shouldProcess(text))
    }

    @Test
    fun filterPipeline_minLengthWithMatcher() {
        val filter = ClipboardFilter.minLength(20)
        val matcher = SocialMediaUrlMatchers.tiktok()

        // Long URL passes filter AND matches
        val url = "https://vm.tiktok.com/ZMF2abc/"
        assertTrue(filter.shouldProcess(url))
        assertTrue(matcher.matches(url))

        // Short text fails filter
        val shortText = "hi"
        assertFalse(filter.shouldProcess(shortText))
    }

    @Test
    fun filterPipeline_excludeSensitiveContent() {
        val filter = ClipboardFilter.exclude(
            Regex("password", RegexOption.IGNORE_CASE),
            Regex("secret", RegexOption.IGNORE_CASE)
        )

        assertTrue(filter.shouldProcess("https://instagram.com/reel/123"))
        assertFalse(filter.shouldProcess("my password is abc123"))
        assertFalse(filter.shouldProcess("secret token: xyz"))
    }

    // ── ClipboardChange + URL Detection ─────────────────────────────

    @Test
    fun clipboardChange_urlDetectionFlow() {
        // Simulate what happens when clipboard changes to an Instagram URL
        val content = "https://www.instagram.com/reel/ABC123/"
        val change = ClipboardChange(
            content = content,
            contentType = ClipboardContentType.Uri,
            timestamp = 1000L,
            source = ClipboardSource.External,
            previousContent = "old text"
        )

        // Verify change properties
        assertTrue(change.isUrl())
        assertTrue(change.hasChanged())
        assertEquals(ClipboardContentType.Uri, change.contentType)

        // Run through matchers
        val matchers = SocialMediaUrlMatchers.all()
        val matchedMatcher = matchers.firstOrNull { it.matches(change.content) }
        assertNotNull(matchedMatcher)
        assertEquals("Instagram", matchedMatcher.name)

        // Extract URL
        val extractedUrl = matchedMatcher.extractUrl(change.content)
        assertNotNull(extractedUrl)

        // Create UrlDetection
        val detection = UrlDetection(
            url = extractedUrl,
            matcher = matchedMatcher,
            change = change
        )
        assertEquals("Instagram", detection.matcher.name)
        assertTrue(detection.url.contains("instagram.com"))
    }

    // ── Worker Trigger ──────────────────────────────────────────────

    @Test
    fun workerTrigger_canBeCreated() {
        val trigger = createClipboardWorkerTrigger()
        assertNotNull(trigger)
        assertTrue(trigger is ClipboardWorkerTrigger)
    }

    @Test
    fun workerTrigger_onUrlDetected_doesNotThrow() {
        val trigger = createClipboardWorkerTrigger()
        val matcher = SocialMediaUrlMatchers.instagram()
        val change = ClipboardChange(
            content = "https://www.instagram.com/reel/ABC123/",
            contentType = ClipboardContentType.Uri,
            timestamp = 1000L,
            source = ClipboardSource.External
        )
        // Should not throw on any platform
        trigger.onUrlDetected(
            url = "https://www.instagram.com/reel/ABC123/",
            matcher = matcher,
            change = change
        )
    }

    // ── Permission ──────────────────────────────────────────────────

    @Test
    fun permission_canBeCreated() {
        val permission = createClipboardPermission()
        assertNotNull(permission)
    }

    @Test
    fun permission_hasClipboardAccess_doesNotThrow() {
        val permission = createClipboardPermission()
        val result = permission.hasClipboardAccess()
        assertNotNull(result.toString())
    }

    @Test
    fun permission_hasOverlayPermission_doesNotThrow() {
        val permission = createClipboardPermission()
        val result = permission.hasOverlayPermission()
        assertNotNull(result.toString())
    }

    @Test
    fun permission_hasNotificationPermission_doesNotThrow() {
        val permission = createClipboardPermission()
        val result = permission.hasNotificationPermission()
        assertNotNull(result.toString())
    }

    // ── Overlay Config ──────────────────────────────────────────────

    @Test
    fun overlayConfig_defaultValues() {
        val config = ClipboardOverlayConfig.Default
        assertEquals(OverlayPosition.BottomEnd, config.position)
        assertTrue(config.draggable)
        assertEquals(0L, config.autoDismissMs)
        assertTrue(config.showOnUrlOnly)
        assertTrue(config.vibrate)
    }

    @Test
    fun overlayConfig_customValues() {
        val config = ClipboardOverlayConfig(
            position = OverlayPosition.TopEnd,
            draggable = false,
            autoDismissMs = 5000L,
            showOnUrlOnly = false,
            vibrate = false
        )
        assertEquals(OverlayPosition.TopEnd, config.position)
        assertFalse(config.draggable)
        assertEquals(5000L, config.autoDismissMs)
    }

    @Test
    fun overlayPosition_allValues() {
        assertEquals(5, OverlayPosition.entries.size)
    }

    // ── Multiple Monitor Instances ──────────────────────────────────

    @Test
    fun multipleMonitors_canCoexist() {
        val monitor1 = createClipboardMonitor()
        val monitor2 = createClipboardMonitor()

        monitor1.addUrlMatcher(SocialMediaUrlMatchers.instagram())
        monitor2.addUrlMatcher(SocialMediaUrlMatchers.youtube())

        monitor1.start()
        monitor2.start()

        assertIs<ClipboardMonitorState.Monitoring>(monitor1.state.value)
        assertIs<ClipboardMonitorState.Monitoring>(monitor2.state.value)

        monitor1.stop()
        monitor2.stop()
    }

    // ── Restart Cycle ───────────────────────────────────────────────

    @Test
    fun monitor_canRestartAfterStop() {
        val monitor = createClipboardMonitor()
        monitor.addUrlMatcher(SocialMediaUrlMatchers.instagram())

        // First cycle
        monitor.start()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)

        // Second cycle
        monitor.start()
        assertIs<ClipboardMonitorState.Monitoring>(monitor.state.value)
        monitor.stop()
        assertIs<ClipboardMonitorState.Idle>(monitor.state.value)
    }
}
