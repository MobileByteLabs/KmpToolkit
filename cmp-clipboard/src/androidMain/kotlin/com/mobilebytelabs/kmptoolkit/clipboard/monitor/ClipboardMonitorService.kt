package com.mobilebytelabs.kmptoolkit.clipboard.monitor

import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardContentType
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardFilter
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardSource
import com.mobilebytelabs.kmptoolkit.clipboard.UrlDetection
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Android Foreground Service that continuously monitors clipboard changes.
 *
 * This is the InSaver-style clipboard monitor service that:
 * 1. Shows a persistent notification ("Clipboard Monitor Service")
 * 2. Listens for clipboard changes via ClipboardManager
 * 3. Applies URL matchers to detect social media URLs
 * 4. Emits changes and URL detections via SharedFlow
 *
 * ## Lifecycle
 *
 * Started by [AndroidClipboardMonitor.start] → runs as foreground service →
 * stopped by [AndroidClipboardMonitor.stop] or notification Stop action.
 */
internal class ClipboardMonitorService : Service() {

    private var clipboardManager: ClipboardManager? = null
    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private var lastContent: String? = null

    companion object {
        /** Shared state accessible by AndroidClipboardMonitor */
        internal val serviceState = MutableStateFlow<ClipboardMonitorServiceState>(
            ClipboardMonitorServiceState.Stopped
        )
        internal val clipboardChanges = MutableSharedFlow<ClipboardChange>(extraBufferCapacity = 64)
        internal val detectedUrls = MutableSharedFlow<UrlDetection>(extraBufferCapacity = 64)

        internal val urlMatchers = mutableListOf<ClipboardUrlMatcher>()
        internal val filters = mutableListOf<ClipboardFilter>()

        internal var notificationTitle = "Clipboard Monitor Service"
        internal var notificationText = "Monitoring clipboard changes"

        const val ACTION_START = "com.mobilebytelabs.kmptoolkit.clipboard.ACTION_START"
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ClipboardNotification.ACTION_PAUSE -> pauseMonitoring()
            ClipboardNotification.ACTION_RESUME -> resumeMonitoring()
            ClipboardNotification.ACTION_STOP -> stopSelf()
            else -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }

    private fun startMonitoring() {
        // Read initial clipboard content
        lastContent = getFromClipboard()

        // Create and register clipboard listener
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            if (serviceState.value is ClipboardMonitorServiceState.Running) {
                onClipboardChanged()
            }
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)

        // Start foreground with notification
        val notification = ClipboardNotification.build(
            context = this,
            title = notificationTitle,
            text = notificationText,
            isPaused = false
        )
        startForeground(ClipboardNotification.NOTIFICATION_ID, notification)

        serviceState.value = ClipboardMonitorServiceState.Running
    }

    private fun stopMonitoring() {
        clipboardListener?.let { listener ->
            clipboardManager?.removePrimaryClipChangedListener(listener)
        }
        clipboardListener = null
        serviceState.value = ClipboardMonitorServiceState.Stopped
    }

    private fun pauseMonitoring() {
        serviceState.value = ClipboardMonitorServiceState.Paused

        // Update notification to show paused state
        val notification = ClipboardNotification.build(
            context = this,
            title = notificationTitle,
            text = notificationText,
            isPaused = true
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(ClipboardNotification.NOTIFICATION_ID, notification)
    }

    private fun resumeMonitoring() {
        serviceState.value = ClipboardMonitorServiceState.Running

        // Update notification to show active state
        val notification = ClipboardNotification.build(
            context = this,
            title = notificationTitle,
            text = notificationText,
            isPaused = false
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(ClipboardNotification.NOTIFICATION_ID, notification)
    }

    private fun onClipboardChanged() {
        val content = getFromClipboard() ?: return

        // Skip if content hasn't changed
        if (content == lastContent) return
        val previousContent = lastContent
        lastContent = content

        // Detect content type
        val contentType = detectContentType(content)

        // Create change event
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
        clipboardChanges.tryEmit(change)

        // Check URL matchers
        for (matcher in urlMatchers) {
            if (matcher.matches(content)) {
                val url = matcher.extractUrl(content) ?: continue
                val detection = UrlDetection(
                    url = url,
                    matcher = matcher,
                    change = change
                )
                detectedUrls.tryEmit(detection)
                break // First match wins
            }
        }
    }

    private fun detectContentType(content: String): ClipboardContentType {
        return when {
            content.startsWith("http://") || content.startsWith("https://") -> ClipboardContentType.Uri
            content.startsWith("<html") || content.startsWith("<HTML") || content.contains("</") -> ClipboardContentType.Html
            else -> ClipboardContentType.Text
        }
    }
}

/**
 * Internal state of the ClipboardMonitorService.
 */
internal sealed class ClipboardMonitorServiceState {
    data object Stopped : ClipboardMonitorServiceState()
    data object Running : ClipboardMonitorServiceState()
    data object Paused : ClipboardMonitorServiceState()
}
