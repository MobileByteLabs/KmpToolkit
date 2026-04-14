package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Continuous clipboard monitor with URL detection and background worker trigger.
 *
 * Unlike [ClipboardObserver] which is lifecycle-aware and foreground-only,
 * [ClipboardMonitor] provides continuous background monitoring with platform-specific
 * implementations:
 *
 * - **Android**: Foreground Service with persistent notification
 * - **iOS**: BGAppRefreshTask + foreground polling
 * - **macOS**: NSTimer continuous polling
 * - **JVM**: Daemon thread with FlavorListener
 * - **JS/Wasm**: Visibility change + async Clipboard API
 * - **Linux**: xclip polling thread
 * - **Windows**: AddClipboardFormatListener (Win32)
 * - **tvOS/watchOS/WASI**: No-op (no clipboard support)
 *
 * ## Usage
 *
 * ```kotlin
 * val monitor = createClipboardMonitor()
 *
 * // Add URL matchers for social media detection
 * monitor.addUrlMatcher(SocialMediaUrlMatchers.instagram())
 * monitor.addUrlMatcher(SocialMediaUrlMatchers.tiktok())
 * monitor.addUrlMatcher(SocialMediaUrlMatchers.youtube())
 *
 * // Start monitoring
 * monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
 *
 * // Collect clipboard changes
 * monitor.changes.collect { change ->
 *     println("Clipboard: ${change.content}")
 *     if (change.isUrl()) {
 *         println("URL detected from ${change.source}")
 *     }
 * }
 *
 * // Stop when done
 * monitor.stop()
 * ```
 *
 * ## URL Detection
 *
 * ```kotlin
 * monitor.urlDetections.collect { (url, matcher) ->
 *     println("Detected ${matcher.name} URL: $url")
 *     // Trigger download, show overlay, etc.
 * }
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardMonitor {
    /** Current state of the monitor. */
    val state: StateFlow<ClipboardMonitorState>

    /** Emits every clipboard change event. */
    val changes: SharedFlow<ClipboardChange>

    /** Most recent clipboard change, or null if none detected yet. */
    val latestChange: StateFlow<ClipboardChange?>

    /** Emits (url, matcher) pairs when a registered URL pattern matches clipboard content. */
    val urlDetections: SharedFlow<UrlDetection>

    /**
     * Starts monitoring clipboard changes.
     *
     * On Android, this starts a foreground service with a persistent notification.
     * On desktop, this starts a background monitoring thread.
     * On iOS, this registers for app lifecycle events and background refresh.
     *
     * @param config Monitor configuration. Defaults to [ClipboardMonitorConfig.Default].
     */
    fun start(config: ClipboardMonitorConfig = ClipboardMonitorConfig.Default)

    /** Stops monitoring and releases all resources. */
    fun stop()

    /** Pauses monitoring temporarily. Can be resumed with [resume]. */
    fun pause()

    /** Resumes monitoring after [pause]. */
    fun resume()

    /**
     * Adds a URL matcher for automatic URL detection.
     *
     * When clipboard content matches any registered matcher's pattern,
     * the match is emitted via [urlDetections].
     */
    fun addUrlMatcher(matcher: ClipboardUrlMatcher)

    /**
     * Adds a content filter to include or exclude clipboard content.
     */
    fun addFilter(filter: ClipboardFilter)
}

/**
 * Result of a URL detection match.
 *
 * @property url The extracted URL.
 * @property matcher The matcher that detected the URL.
 * @property change The full clipboard change event.
 */
data class UrlDetection(val url: String, val matcher: ClipboardUrlMatcher, val change: ClipboardChange)

/**
 * Creates a platform-specific [ClipboardMonitor] instance.
 *
 * @return A new ClipboardMonitor ready to be configured and started.
 * @since 0.2.0
 */
expect fun createClipboardMonitor(): ClipboardMonitor
