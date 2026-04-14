package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Unified clipboard API — single entry point for all clipboard operations.
 *
 * Combines manual copy/paste, observation, monitoring, async operations,
 * URL detection, and history into one interface. Automatically resolves
 * the right implementation based on configuration.
 *
 * ## Quick Start (Minimal)
 *
 * ```kotlin
 * val clipboard = ClipboardManager()
 *
 * clipboard.copy("Hello!")
 * val text = clipboard.paste()
 * ```
 *
 * ## Full Featured
 *
 * ```kotlin
 * val clipboard = ClipboardManager(
 *     ClipboardManagerConfig(
 *         observe = true,           // auto-observe clipboard changes
 *         historySize = 20,         // keep last 20 entries
 *         detectUrls = true,        // enable URL matching
 *         urlMatchers = SocialMediaUrlMatchers.all(),
 *         async = true              // prefer async operations
 *     )
 * )
 *
 * clipboard.start()
 *
 * // All in one place
 * clipboard.content.collect { text -> println("Clipboard: $text") }
 * clipboard.history.collect { entries -> println("${entries.size} items") }
 * clipboard.urlDetections.collect { det -> println("URL: ${det.url}") }
 *
 * clipboard.stop()
 * ```
 *
 * ## Compose
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val clipboard = rememberClipboardManager()
 *     val content by clipboard.content.collectAsState()
 *     val history by clipboard.history.collectAsState()
 *
 *     Text("Clipboard: $content")
 *     Text("History: ${history.size} items")
 * }
 * ```
 *
 * @since 0.2.0
 */
class ClipboardManager(
    private val config: ClipboardManagerConfig = ClipboardManagerConfig.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Underlying implementations (lazy — created only when needed)
    private val observer: ClipboardObserver by lazy { createClipboardObserver() }
    private val monitor: ClipboardMonitor by lazy {
        createClipboardMonitor().also { mon ->
            config.urlMatchers.forEach { mon.addUrlMatcher(it) }
            config.filters.forEach { mon.addFilter(it) }
        }
    }
    private val historyManager: ClipboardHistory by lazy {
        createClipboardHistory(config.historySize)
    }
    private val permission: ClipboardPermission by lazy { createClipboardPermission() }

    private var started = false

    // ── Content (Auto-Observing StateFlow) ──────────────────────

    /**
     * Current clipboard content as a StateFlow.
     *
     * Automatically updated when:
     * - `observe = true` in config (uses ClipboardObserver)
     * - `detectUrls = true` in config (uses ClipboardMonitor)
     * - App returns to foreground (lifecycle-aware)
     *
     * Call [start] to begin observation, or use [rememberClipboardManager] in Compose.
     */
    val content: StateFlow<String?> get() = observer.clipboardContent

    // ── History ─────────────────────────────────────────────────

    /**
     * Clipboard history entries (newest first).
     *
     * Automatically populated when `historySize > 0` and [start] is called.
     */
    val history: StateFlow<List<ClipboardHistoryEntry>>
        get() = historyManager.entries

    /**
     * Maximum history entries to keep.
     */
    val historyMaxSize: Int get() = config.historySize

    // ── Monitor State ───────────────────────────────────────────

    /**
     * Monitor state — Idle, Monitoring, Paused, or Error.
     *
     * Only active when `detectUrls = true`.
     */
    val monitorState: StateFlow<ClipboardMonitorState> get() = monitor.state

    /**
     * URL detections from clipboard content.
     *
     * Emits when clipboard content matches any registered URL matcher.
     * Only active when `detectUrls = true` and [start] is called.
     */
    val urlDetections: SharedFlow<UrlDetection> get() = monitor.urlDetections

    /**
     * All clipboard change events with metadata.
     *
     * Only active when `detectUrls = true` and [start] is called.
     */
    val changes: SharedFlow<ClipboardChange> get() = monitor.changes

    /**
     * Most recent clipboard change with metadata, or null.
     */
    val latestChange: StateFlow<ClipboardChange?> get() = monitor.latestChange

    // ── Sync Operations ─────────────────────────────────────────

    /**
     * Copy text to clipboard (synchronous).
     *
     * @return true if copy succeeded.
     */
    fun copy(text: String): Boolean = copyToClipboard(text)

    /**
     * Read text from clipboard (synchronous).
     *
     * @return Clipboard text, or null if empty/unavailable.
     * Note: Returns null on JS/Wasm — use [pasteAsync] instead.
     */
    fun paste(): String? = getFromClipboard()

    /**
     * Check if clipboard has text (synchronous).
     */
    fun hasText(): Boolean = hasClipboardText()

    /**
     * Clear the clipboard.
     */
    fun clear() = clearClipboard()

    // ── Async Operations ────────────────────────────────────────

    /**
     * Copy text to clipboard (async — works on all platforms including JS/Wasm).
     */
    suspend fun copyAsync(text: String): Boolean = copyToClipboardAsync(text)

    /**
     * Read text from clipboard (async — works on all platforms including JS/Wasm).
     */
    suspend fun pasteAsync(): String? = getFromClipboardAsync()

    /**
     * Check if clipboard has text (async).
     */
    suspend fun hasTextAsync(): Boolean = hasClipboardTextAsync()

    // ── History Operations ──────────────────────────────────────

    /**
     * Copy a history entry back to the system clipboard.
     */
    fun copyFromHistory(entry: ClipboardHistoryEntry): Boolean =
        historyManager.copyToClipboard(entry)

    /**
     * Remove a specific entry from history.
     */
    fun removeFromHistory(entry: ClipboardHistoryEntry) =
        historyManager.remove(entry)

    /**
     * Clear all clipboard history.
     */
    fun clearHistory() = historyManager.clear()

    // ── URL Matchers & Filters ──────────────────────────────────

    /**
     * Add a URL matcher for automatic URL detection.
     */
    fun addUrlMatcher(matcher: ClipboardUrlMatcher) {
        monitor.addUrlMatcher(matcher)
    }

    /**
     * Add all built-in social media URL matchers.
     */
    fun addAllSocialMediaMatchers() {
        SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
    }

    /**
     * Add a content filter.
     */
    fun addFilter(filter: ClipboardFilter) {
        monitor.addFilter(filter)
    }

    // ── Permissions ─────────────────────────────────────────────

    /**
     * Whether clipboard access is available on this platform.
     */
    fun hasClipboardAccess(): Boolean = permission.hasClipboardAccess()

    /**
     * Whether overlay (floating FAB) permission is granted.
     */
    fun hasOverlayPermission(): Boolean = permission.hasOverlayPermission()

    /**
     * Whether notification permission is granted.
     */
    fun hasNotificationPermission(): Boolean = permission.hasNotificationPermission()

    /**
     * Request notification permission.
     */
    suspend fun requestNotificationPermission(): Boolean =
        permission.requestNotificationPermission()

    // ── Lifecycle ───────────────────────────────────────────────

    /**
     * Start all configured features (observer, monitor, history).
     *
     * Call this when ready to begin clipboard tracking. In Compose,
     * use [rememberClipboardManager] which calls this automatically.
     */
    fun start() {
        if (started) return
        started = true

        // Always start observer for content StateFlow
        if (config.observe) {
            observer.startObserving()
        }

        // Start monitor if URL detection is enabled
        if (config.detectUrls) {
            monitor.start(ClipboardMonitorConfig(
                pollingIntervalMs = config.pollingIntervalMs,
                detectUrls = true,
                showNotification = config.showNotification,
                showOverlay = config.showOverlay,
                triggerWorkerOnUrl = config.triggerWorkerOnUrl,
            ))
        }

        // Start history if enabled
        if (config.historySize > 0) {
            historyManager.startCapturing()
        }
    }

    /**
     * Stop all features and release resources.
     */
    fun stop() {
        if (!started) return
        started = false

        observer.stopObserving()

        if (config.detectUrls) {
            monitor.stop()
        }

        if (config.historySize > 0) {
            historyManager.stopCapturing()
        }
    }

    /**
     * Pause monitoring (keeps observer active).
     */
    fun pause() {
        if (config.detectUrls) monitor.pause()
    }

    /**
     * Resume monitoring after pause.
     */
    fun resume() {
        if (config.detectUrls) monitor.resume()
    }

    /** Whether the manager is currently active. */
    val isActive: Boolean get() = started
}

/**
 * Configuration for [ClipboardManager].
 *
 * @property observe Enable auto-observation (StateFlow updates on clipboard change).
 * @property historySize Number of clipboard entries to keep (0 = disabled).
 * @property detectUrls Enable URL pattern matching.
 * @property urlMatchers URL matchers for detection (e.g., SocialMediaUrlMatchers.all()).
 * @property filters Content filters to apply before processing.
 * @property async Prefer async operations internally.
 * @property pollingIntervalMs Polling interval for platforms without native listeners.
 * @property showNotification Show persistent notification (Android ForegroundService).
 * @property showOverlay Show floating FAB overlay on URL detection.
 * @property triggerWorkerOnUrl Auto-trigger background worker on URL match.
 * @since 0.2.0
 */
data class ClipboardManagerConfig(
    val observe: Boolean = true,
    val historySize: Int = 20,
    val detectUrls: Boolean = false,
    val urlMatchers: List<ClipboardUrlMatcher> = emptyList(),
    val filters: List<ClipboardFilter> = emptyList(),
    val async: Boolean = false,
    val pollingIntervalMs: Long = 1000L,
    val showNotification: Boolean = false,
    val showOverlay: Boolean = false,
    val triggerWorkerOnUrl: Boolean = false
) {
    companion object {
        /** Minimal config — observe + history, no URL detection. */
        val Default = ClipboardManagerConfig()

        /** Full featured — observe + history + all social media URL detection. */
        val Full = ClipboardManagerConfig(
            observe = true,
            historySize = 30,
            detectUrls = true,
            urlMatchers = SocialMediaUrlMatchers.all(),
            async = true
        )

        /** Social media downloader — URL detection + notification + worker trigger. */
        val SocialMediaDownloader = ClipboardManagerConfig(
            observe = true,
            historySize = 20,
            detectUrls = true,
            urlMatchers = SocialMediaUrlMatchers.all(),
            filters = listOf(ClipboardFilter.urlOnly()),
            showNotification = true,
            showOverlay = true,
            triggerWorkerOnUrl = true
        )
    }
}
