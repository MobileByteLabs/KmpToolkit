package com.mobilebytelabs.kmptoolkit.clipboard.monitor

/**
 * Configuration for [ClipboardMonitor][com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitor].
 *
 * Controls polling behavior, URL detection, filtering, notifications, and overlay.
 *
 * ## Usage
 *
 * ```kotlin
 * // Default config — basic monitoring
 * val monitor = createClipboardMonitor()
 * monitor.start(ClipboardMonitorConfig.Default)
 *
 * // Social media downloader config
 * monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)
 *
 * // Custom config
 * monitor.start(ClipboardMonitorConfig(
 *     pollingIntervalMs = 500L,
 *     detectUrls = true,
 *     showNotification = true,
 *     showOverlay = true,
 *     triggerWorkerOnUrl = true
 * ))
 * ```
 *
 * @property pollingIntervalMs How often to poll clipboard on platforms without native listeners (ms).
 * @property detectUrls Whether to automatically detect URLs in clipboard content.
 * @property showNotification Whether to show a persistent notification (Android foreground service).
 * @property showOverlay Whether to show a floating FAB overlay when URL is detected.
 * @property triggerWorkerOnUrl Whether to automatically trigger a background worker when a URL is detected.
 * @property notificationTitle Custom notification title (Android).
 * @property notificationText Custom notification text (Android).
 * @since 0.2.0
 */
data class ClipboardMonitorConfig(
    val pollingIntervalMs: Long = 1000L,
    val detectUrls: Boolean = true,
    val showNotification: Boolean = true,
    val showOverlay: Boolean = false,
    val triggerWorkerOnUrl: Boolean = false,
    val notificationTitle: String = "Clipboard Monitor Service",
    val notificationText: String = "Monitoring clipboard changes"
) {
    companion object {
        /** Default configuration — basic monitoring with notification. */
        val Default = ClipboardMonitorConfig()

        /** Optimized for social media URL detection and auto-download. */
        val SocialMediaDownloader = ClipboardMonitorConfig(
            pollingIntervalMs = 500L,
            detectUrls = true,
            showNotification = true,
            showOverlay = true,
            triggerWorkerOnUrl = true
        )
    }
}
