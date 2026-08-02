package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTimer
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIPasteboard

internal class IosClipboardMonitor : ClipboardMonitor {
    private val _state = MutableStateFlow<ClipboardMonitorState>(ClipboardMonitorState.Idle)
    override val state: StateFlow<ClipboardMonitorState> = _state.asStateFlow()

    private val _changes = MutableSharedFlow<ClipboardChange>(extraBufferCapacity = 64)
    override val changes: SharedFlow<ClipboardChange> = _changes.asSharedFlow()

    private val _latestChange = MutableStateFlow<ClipboardChange?>(null)
    override val latestChange: StateFlow<ClipboardChange?> = _latestChange.asStateFlow()

    private val _urlDetections = MutableSharedFlow<UrlDetection>(extraBufferCapacity = 64)
    override val urlDetections: SharedFlow<UrlDetection> = _urlDetections.asSharedFlow()

    private val urlMatchers = mutableListOf<ClipboardUrlMatcher>()
    private val filters = mutableListOf<ClipboardFilter>()

    private var lastChangeCount: Long = 0
    private var lastContent: String? = null
    private var pollingTimer: NSTimer? = null
    private var foregroundObserver: Any? = null

    override fun start(config: ClipboardMonitorConfig) {
        if (_state.value is ClipboardMonitorState.Monitoring) return

        // Read changeCount only — avoids triggering iOS 16+ clipboard-access permission
        // dialog on startup. Content is read lazily in checkClipboard() on first detected change.
        lastChangeCount = UIPasteboard.generalPasteboard.changeCount
        lastContent = null

        // Register for foreground notification
        foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                if (_state.value is ClipboardMonitorState.Monitoring) {
                    checkClipboard()
                }
            },
        )

        // Start polling timer for continuous monitoring
        val intervalSeconds = config.pollingIntervalMs / 1000.0
        pollingTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = intervalSeconds,
            repeats = true,
            block = { _ ->
                if (_state.value is ClipboardMonitorState.Monitoring) {
                    checkClipboard()
                }
            },
        )

        _state.value = ClipboardMonitorState.Monitoring
    }

    override fun stop() {
        pollingTimer?.invalidate()
        pollingTimer = null

        foregroundObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
        foregroundObserver = null

        lastContent = null
        _state.value = ClipboardMonitorState.Idle
    }

    override fun pause() {
        if (_state.value is ClipboardMonitorState.Monitoring) {
            _state.value = ClipboardMonitorState.Paused
        }
    }

    override fun resume() {
        if (_state.value is ClipboardMonitorState.Paused) {
            _state.value = ClipboardMonitorState.Monitoring
        }
    }

    override fun addUrlMatcher(matcher: ClipboardUrlMatcher) {
        urlMatchers.add(matcher)
    }

    override fun addFilter(filter: ClipboardFilter) {
        filters.add(filter)
    }

    private fun checkClipboard() {
        val currentChangeCount = UIPasteboard.generalPasteboard.changeCount
        if (currentChangeCount == lastChangeCount) return

        lastChangeCount = currentChangeCount
        val content = UIPasteboard.generalPasteboard.string ?: return

        if (content == lastContent) return
        val previousContent = lastContent
        lastContent = content

        // Detect content type
        val contentType = when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardContentType.Uri
            content.startsWith("<html") || content.contains("</") -> ClipboardContentType.Html
            else -> ClipboardContentType.Text
        }

        val change = ClipboardChange(
            content = content,
            contentType = contentType,
            timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            source = ClipboardSource.External,
            previousContent = previousContent,
        )

        // Apply filters
        if (filters.any { !it.shouldProcess(content) }) return

        _changes.tryEmit(change)
        _latestChange.value = change

        // Check URL matchers
        for (matcher in urlMatchers) {
            if (matcher.matches(content)) {
                val url = matcher.extractUrl(content) ?: continue
                _urlDetections.tryEmit(UrlDetection(url = url, matcher = matcher, change = change))
                break
            }
        }
    }
}

actual fun createClipboardMonitor(): ClipboardMonitor = IosClipboardMonitor()
