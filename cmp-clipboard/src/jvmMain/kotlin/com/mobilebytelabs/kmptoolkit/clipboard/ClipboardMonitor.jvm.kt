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
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorEvent
import java.awt.datatransfer.FlavorListener

internal class JvmClipboardMonitor :
    ClipboardMonitor,
    FlavorListener {
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
    private var pollingThread: Thread? = null

    @Volatile private var polling = false

    private val clipboard by lazy {
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (e: Exception) {
            null
        }
    }

    override fun start(config: ClipboardMonitorConfig) {
        if (_state.value is ClipboardMonitorState.Monitoring) return

        lastContent = readClipboardText()

        // Register FlavorListener for native change events
        try {
            clipboard?.addFlavorListener(this)
        } catch (e: Exception) { /* headless */ }

        // Start polling thread as backup (FlavorListener can be unreliable)
        polling = true
        pollingThread = Thread({
            while (polling) {
                try {
                    Thread.sleep(config.pollingIntervalMs)
                    if (_state.value is ClipboardMonitorState.Monitoring) {
                        checkClipboard()
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        }, "ClipboardMonitor-Poll").apply {
            isDaemon = true
            start()
        }

        _state.value = ClipboardMonitorState.Monitoring
    }

    override fun stop() {
        polling = false
        pollingThread?.interrupt()
        pollingThread = null

        try {
            clipboard?.removeFlavorListener(this)
        } catch (e: Exception) { }

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

    override fun flavorsChanged(e: FlavorEvent?) {
        if (_state.value is ClipboardMonitorState.Monitoring) {
            checkClipboard()
        }
    }

    private fun checkClipboard() {
        val content = readClipboardText() ?: return
        if (content == lastContent) return
        val previousContent = lastContent
        lastContent = content

        val contentType = when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardContentType.Uri
            content.startsWith("<html") || content.contains("</") -> ClipboardContentType.Html
            else -> ClipboardContentType.Text
        }

        val change = ClipboardChange(
            content = content,
            contentType = contentType,
            timestamp = System.currentTimeMillis(),
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

    private fun readClipboardText(): String? = try {
        val cb = clipboard ?: return null
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            cb.getData(DataFlavor.stringFlavor) as? String
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

actual fun createClipboardMonitor(): ClipboardMonitor = JvmClipboardMonitor()
