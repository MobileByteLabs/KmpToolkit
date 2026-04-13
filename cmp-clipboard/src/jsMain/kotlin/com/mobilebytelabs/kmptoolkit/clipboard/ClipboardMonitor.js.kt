package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event

internal class JsClipboardMonitor : ClipboardMonitor {
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

    private var lastContent: String? = null
    private var visibilityHandler: ((Event) -> Unit)? = null
    private var focusHandler: ((Event) -> Unit)? = null
    private var pollingIntervalId: Int? = null

    override fun start(config: ClipboardMonitorConfig) {
        if (_state.value is ClipboardMonitorState.Monitoring) return

        // Listen for visibility change (tab focus)
        visibilityHandler = { _ ->
            if (_state.value is ClipboardMonitorState.Monitoring) {
                val visible = js("document.visibilityState") as? String
                if (visible == "visible") {
                    tryReadClipboard()
                }
            }
        }
        document.addEventListener("visibilitychange", visibilityHandler!!)

        // Listen for window focus
        focusHandler = { _ ->
            if (_state.value is ClipboardMonitorState.Monitoring) {
                tryReadClipboard()
            }
        }
        window.addEventListener("focus", focusHandler!!)

        // Polling interval (async clipboard read attempt)
        pollingIntervalId = window.setInterval({
            if (_state.value is ClipboardMonitorState.Monitoring) {
                tryReadClipboard()
            }
        }, config.pollingIntervalMs.toInt())

        _state.value = ClipboardMonitorState.Monitoring
    }

    override fun stop() {
        visibilityHandler?.let { document.removeEventListener("visibilitychange", it) }
        focusHandler?.let { window.removeEventListener("focus", it) }
        pollingIntervalId?.let { window.clearInterval(it) }

        visibilityHandler = null
        focusHandler = null
        pollingIntervalId = null
        lastContent = null
        _state.value = ClipboardMonitorState.Idle
    }

    override fun pause() {
        if (_state.value is ClipboardMonitorState.Monitoring) _state.value = ClipboardMonitorState.Paused
    }

    override fun resume() {
        if (_state.value is ClipboardMonitorState.Paused) _state.value = ClipboardMonitorState.Monitoring
    }

    override fun addUrlMatcher(matcher: ClipboardUrlMatcher) { urlMatchers.add(matcher) }
    override fun addFilter(filter: ClipboardFilter) { filters.add(filter) }

    private fun tryReadClipboard() {
        // Attempt async read via navigator.clipboard
        val clipboard = js("navigator.clipboard")
        if (clipboard != null) {
            val promise = clipboard.readText()
            promise.then { text: String ->
                processContent(text)
            }.catch { _: Throwable ->
                // Permission denied — can't read clipboard in JS without user gesture
            }
        }
    }

    private fun processContent(content: String) {
        if (content == lastContent || content.isEmpty()) return
        val previousContent = lastContent
        lastContent = content

        val contentType = when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardContentType.Uri
            else -> ClipboardContentType.Text
        }

        val change = ClipboardChange(
            content = content,
            contentType = contentType,
            timestamp = js("Date.now()") as Long,
            source = ClipboardSource.External,
            previousContent = previousContent
        )

        if (filters.any { !it.shouldProcess(content) }) return

        _changes.tryEmit(change)
        _latestChange.value = change

        for (matcher in urlMatchers) {
            if (matcher.matches(content)) {
                val url = matcher.extractUrl(content) ?: continue
                _urlDetections.tryEmit(UrlDetection(url = url, matcher = matcher, change = change))
                break
            }
        }
    }
}

actual fun createClipboardMonitor(): ClipboardMonitor = JsClipboardMonitor()
