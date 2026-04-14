package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wasm JS implementation of ClipboardMonitor.
 *
 * Uses visibility change events and periodic async clipboard reads via
 * `navigator.clipboard.readText()` to detect clipboard changes.
 *
 * ## Limitations
 *
 * - Clipboard read requires a user gesture or Permissions API grant
 * - Monitoring only works when the browser tab is visible/focused
 * - Some browsers may block clipboard reads without user interaction
 */

@JsFun("(callback) => { document.addEventListener('visibilitychange', callback); }")
private external fun jsAddVisibilityListener(callback: JsAny)

@JsFun("(callback) => { document.removeEventListener('visibilitychange', callback); }")
private external fun jsRemoveVisibilityListener(callback: JsAny)

@JsFun("() => { return document.visibilityState === 'visible'; }")
private external fun jsIsVisible(): Boolean

@JsFun("() => { return Date.now(); }")
private external fun jsDateNow(): Double

internal class WasmJsClipboardMonitor : ClipboardMonitor {
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    override fun start(config: ClipboardMonitorConfig) {
        if (_state.value is ClipboardMonitorState.Monitoring) return

        // Start polling coroutine that attempts async clipboard reads
        pollingJob = scope.launch {
            while (isActive) {
                if (_state.value is ClipboardMonitorState.Monitoring) {
                    tryReadAndProcess()
                }
                delay(config.pollingIntervalMs)
            }
        }

        _state.value = ClipboardMonitorState.Monitoring
    }

    override fun stop() {
        pollingJob?.cancel()
        pollingJob = null
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

    private suspend fun tryReadAndProcess() {
        val content = try {
            getFromClipboardAsync()
        } catch (e: Throwable) {
            null
        } ?: return

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
            timestamp = jsDateNow().toLong(),
            source = ClipboardSource.External,
            previousContent = previousContent,
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

actual fun createClipboardMonitor(): ClipboardMonitor = WasmJsClipboardMonitor()
