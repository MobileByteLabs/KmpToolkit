package com.mobilebytelabs.kmptoolkit.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardBootReceiver
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorService
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.ClipboardOverlay
import com.mobilebytelabs.kmptoolkit.clipboard.worker.createClipboardWorkerTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android implementation of ClipboardMonitor.
 *
 * Uses a **dual-mode approach**:
 * 1. **In-process mode** (always): ClipboardManager listener + lifecycle observer
 *    for reliable clipboard detection within the app.
 * 2. **Service mode** (optional): ForegroundService with persistent notification
 *    for background monitoring when `showNotification = true`.
 *
 * The in-process mode works identically to ClipboardObserver but adds URL matching,
 * filtering, and change metadata. It works even if the service fails to start.
 */
internal class AndroidClipboardMonitor : ClipboardMonitor, DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: ClipboardOverlay? = null
    private var config: ClipboardMonitorConfig? = null

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

    // In-process clipboard monitoring
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var lastContent: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var serviceStarted = false

    private val clipboardManager: ClipboardManager?
        get() = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    init {
        // Forward URL detections to overlay and worker trigger
        scope.launch {
            urlDetections.collect { detection ->
                overlay?.onUrlDetected(detection)
                if (config?.triggerWorkerOnUrl == true) {
                    val trigger = createClipboardWorkerTrigger()
                    trigger.onUrlDetected(detection.url, detection.matcher, detection.change)
                }
            }
        }
    }

    override fun start(config: ClipboardMonitorConfig) {
        val context = appContext ?: return
        if (_state.value is ClipboardMonitorState.Monitoring) return

        this.config = config
        lastContent = getFromClipboard()

        // === Mode 1: Always start in-process monitoring ===
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (_state.value is ClipboardMonitorState.Monitoring) {
                onClipboardChanged()
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)

        // Register lifecycle for foreground detection (same fix as Observer)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        _state.value = ClipboardMonitorState.Monitoring

        // === Mode 2: Optionally start foreground service ===
        if (config.showNotification) {
            tryStartService(context, config)
        }

        // Show overlay if configured
        if (config.showOverlay) {
            overlay = ClipboardOverlay().also { it.show() }
        }
    }

    override fun stop() {
        val context = appContext ?: return

        // Stop in-process monitoring
        clipboardListener?.let { clipboardManager?.removePrimaryClipChangedListener(it) }
        clipboardListener = null
        handler.removeCallbacksAndMessages(null)

        try {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        } catch (e: Exception) { }

        // Stop service if started
        if (serviceStarted) {
            context.stopService(Intent(context, ClipboardMonitorService::class.java))
            ClipboardBootReceiver.setWasRunning(context, false)
            serviceStarted = false
        }

        // Hide overlay
        overlay?.hide()
        overlay = null

        lastContent = null
        config = null
        _state.value = ClipboardMonitorState.Idle
    }

    override fun pause() {
        if (_state.value is ClipboardMonitorState.Monitoring) {
            _state.value = ClipboardMonitorState.Paused
            if (serviceStarted) {
                val context = appContext ?: return
                val intent = Intent(context, ClipboardMonitorService::class.java).apply {
                    action = com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardNotification.ACTION_PAUSE
                }
                context.startService(intent)
            }
        }
    }

    override fun resume() {
        if (_state.value is ClipboardMonitorState.Paused) {
            _state.value = ClipboardMonitorState.Monitoring
            if (serviceStarted) {
                val context = appContext ?: return
                val intent = Intent(context, ClipboardMonitorService::class.java).apply {
                    action = com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardNotification.ACTION_RESUME
                }
                context.startService(intent)
            }
        }
    }

    override fun addUrlMatcher(matcher: ClipboardUrlMatcher) {
        urlMatchers.add(matcher)
        if (serviceStarted) ClipboardMonitorService.urlMatchers.add(matcher)
    }

    override fun addFilter(filter: ClipboardFilter) {
        filters.add(filter)
        if (serviceStarted) ClipboardMonitorService.filters.add(filter)
    }

    // ── Lifecycle (foreground detection) ────────────────────────────

    override fun onStart(owner: LifecycleOwner) {
        if (_state.value is ClipboardMonitorState.Monitoring) {
            onClipboardChanged()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (_state.value is ClipboardMonitorState.Monitoring) {
            onClipboardChanged()
            handler.postDelayed({ if (_state.value is ClipboardMonitorState.Monitoring) onClipboardChanged() }, 100)
            handler.postDelayed({ if (_state.value is ClipboardMonitorState.Monitoring) onClipboardChanged() }, 300)
            handler.postDelayed({ if (_state.value is ClipboardMonitorState.Monitoring) onClipboardChanged() }, 600)
        }
    }

    // ── Clipboard change processing ────────────────────────────────

    private fun onClipboardChanged() {
        val content = getFromClipboard() ?: return
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
            timestamp = System.currentTimeMillis(),
            source = ClipboardSource.External,
            previousContent = previousContent
        )

        // Apply filters
        if (filters.any { !it.shouldProcess(content) }) return

        // Emit change
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

    // ── Service (optional background mode) ─────────────────────────

    private fun tryStartService(context: Context, config: ClipboardMonitorConfig) {
        ClipboardMonitorService.urlMatchers.clear()
        ClipboardMonitorService.urlMatchers.addAll(urlMatchers)
        ClipboardMonitorService.filters.clear()
        ClipboardMonitorService.filters.addAll(filters)
        ClipboardMonitorService.notificationTitle = config.notificationTitle
        ClipboardMonitorService.notificationText = config.notificationText

        val intent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = ClipboardMonitorService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            serviceStarted = true
            ClipboardBootReceiver.setWasRunning(context, true)
        } catch (e: Exception) {
            // Service failed — in-process mode still works
            serviceStarted = false
        }
    }
}

actual fun createClipboardMonitor(): ClipboardMonitor = AndroidClipboardMonitor()
