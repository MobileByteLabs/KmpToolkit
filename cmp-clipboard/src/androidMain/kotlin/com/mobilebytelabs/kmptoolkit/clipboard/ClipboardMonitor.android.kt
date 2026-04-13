package com.mobilebytelabs.kmptoolkit.clipboard

import android.content.Intent
import android.os.Build
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardBootReceiver
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorConfig
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorService
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorServiceState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.ClipboardOverlay
import com.mobilebytelabs.kmptoolkit.clipboard.overlay.ClipboardOverlayConfig
import com.mobilebytelabs.kmptoolkit.clipboard.worker.AndroidClipboardWorkerTrigger
import com.mobilebytelabs.kmptoolkit.clipboard.worker.createClipboardWorkerTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Android implementation of ClipboardMonitor.
 *
 * Wraps [ClipboardMonitorService] (foreground service) and provides:
 * - Service lifecycle management (start/stop/pause/resume)
 * - Floating FAB overlay via [ClipboardOverlay]
 * - URL detection forwarding to [AndroidClipboardWorkerTrigger]
 * - State mapping from service state to monitor state
 *
 * ## Architecture
 *
 * ```
 * AndroidClipboardMonitor (API layer)
 *   ├── ClipboardMonitorService (foreground service + clipboard listener)
 *   ├── ClipboardOverlay (floating FAB window)
 *   └── AndroidClipboardWorkerTrigger (background worker bridge)
 * ```
 */
internal class AndroidClipboardMonitor : ClipboardMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlay: ClipboardOverlay? = null
    private var config: ClipboardMonitorConfig? = null

    override val state: StateFlow<ClipboardMonitorState> =
        ClipboardMonitorService.serviceState
            .map { serviceState ->
                when (serviceState) {
                    is ClipboardMonitorServiceState.Stopped -> ClipboardMonitorState.Idle
                    is ClipboardMonitorServiceState.Running -> ClipboardMonitorState.Monitoring
                    is ClipboardMonitorServiceState.Paused -> ClipboardMonitorState.Paused
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, ClipboardMonitorState.Idle)

    override val changes: SharedFlow<ClipboardChange> = ClipboardMonitorService.clipboardChanges

    private val _latestChange = kotlinx.coroutines.flow.MutableStateFlow<ClipboardChange?>(null)
    override val latestChange: StateFlow<ClipboardChange?> = _latestChange

    override val urlDetections: SharedFlow<UrlDetection> = ClipboardMonitorService.detectedUrls

    private val urlMatchers = mutableListOf<ClipboardUrlMatcher>()
    private val filters = mutableListOf<ClipboardFilter>()

    init {
        // Forward changes to latestChange
        scope.launch {
            changes.collect { change ->
                _latestChange.value = change
            }
        }

        // Forward URL detections to overlay and worker trigger
        scope.launch {
            urlDetections.collect { detection ->
                overlay?.onUrlDetected(detection)

                // Trigger worker if configured
                if (config?.triggerWorkerOnUrl == true) {
                    val trigger = createClipboardWorkerTrigger()
                    trigger.onUrlDetected(detection.url, detection.matcher, detection.change)
                }
            }
        }
    }

    override fun start(config: ClipboardMonitorConfig) {
        val context = appContext ?: run {
            return
        }
        if (state.value is ClipboardMonitorState.Monitoring) return

        this.config = config

        // Pass matchers and filters to service
        ClipboardMonitorService.urlMatchers.clear()
        ClipboardMonitorService.urlMatchers.addAll(urlMatchers)
        ClipboardMonitorService.filters.clear()
        ClipboardMonitorService.filters.addAll(filters)
        ClipboardMonitorService.notificationTitle = config.notificationTitle
        ClipboardMonitorService.notificationText = config.notificationText

        // Start foreground service
        val serviceIntent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = ClipboardMonitorService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Service start may fail if battery optimization is aggressive
            return
        }

        // Track running state for boot receiver
        ClipboardBootReceiver.setWasRunning(context, true)

        // Show overlay if configured
        if (config.showOverlay) {
            overlay = ClipboardOverlay().also { it.show() }
        }
    }

    override fun stop() {
        val context = appContext ?: return

        // Stop service
        val serviceIntent = Intent(context, ClipboardMonitorService::class.java)
        context.stopService(serviceIntent)

        // Hide overlay
        overlay?.hide()
        overlay = null

        // Track state for boot receiver
        ClipboardBootReceiver.setWasRunning(context, false)

        config = null
    }

    override fun pause() {
        val context = appContext ?: return
        val pauseIntent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardNotification.ACTION_PAUSE
        }
        context.startService(pauseIntent)
    }

    override fun resume() {
        val context = appContext ?: return
        val resumeIntent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardNotification.ACTION_RESUME
        }
        context.startService(resumeIntent)
    }

    override fun addUrlMatcher(matcher: ClipboardUrlMatcher) {
        urlMatchers.add(matcher)
        // Also update service if already running
        ClipboardMonitorService.urlMatchers.add(matcher)
    }

    override fun addFilter(filter: ClipboardFilter) {
        filters.add(filter)
        ClipboardMonitorService.filters.add(filter)
    }
}

actual fun createClipboardMonitor(): ClipboardMonitor = AndroidClipboardMonitor()
