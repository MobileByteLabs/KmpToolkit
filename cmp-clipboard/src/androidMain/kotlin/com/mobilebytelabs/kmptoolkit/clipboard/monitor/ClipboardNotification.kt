package com.mobilebytelabs.kmptoolkit.clipboard.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Manages notification channel and notification builder for the clipboard monitor service.
 *
 * Creates a persistent notification required for Android foreground services.
 * The notification shows "Clipboard Monitor Service" with Pause/Stop actions.
 */
internal object ClipboardNotification {

    const val CHANNEL_ID = "kmptoolkit_clipboard_monitor"
    const val NOTIFICATION_ID = 9901

    internal const val ACTION_PAUSE = "com.mobilebytelabs.kmptoolkit.clipboard.ACTION_PAUSE"
    internal const val ACTION_RESUME = "com.mobilebytelabs.kmptoolkit.clipboard.ACTION_RESUME"
    internal const val ACTION_STOP = "com.mobilebytelabs.kmptoolkit.clipboard.ACTION_STOP"

    /**
     * Creates the notification channel (required for Android 8.0+).
     */
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipboard Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors clipboard for URLs and triggers background processing"
                setShowBadge(false)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the foreground service notification.
     *
     * @param context Application context.
     * @param title Notification title (default: "Clipboard Monitor Service").
     * @param text Notification text (default: "Monitoring clipboard changes").
     * @param isPaused Whether the monitor is currently paused.
     * @param launchIntent Optional PendingIntent to launch when notification is tapped.
     */
    fun build(
        context: Context,
        title: String = "Clipboard Monitor Service",
        text: String = "Monitoring clipboard changes",
        isPaused: Boolean = false,
        launchIntent: PendingIntent? = null
    ): Notification {
        createChannel(context)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Pause/Resume action
        val toggleAction = if (isPaused) {
            val resumeIntent = Intent(context, ClipboardMonitorService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePending = PendingIntent.getService(context, 1, resumeIntent, flags)
            NotificationCompat.Action.Builder(0, "Resume", resumePending).build()
        } else {
            val pauseIntent = Intent(context, ClipboardMonitorService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePending = PendingIntent.getService(context, 1, pauseIntent, flags)
            NotificationCompat.Action.Builder(0, "Pause", pausePending).build()
        }

        // Stop action
        val stopIntent = Intent(context, ClipboardMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(context, 2, stopIntent, flags)
        val stopAction = NotificationCompat.Action.Builder(0, "Stop", stopPending).build()

        val displayText = if (isPaused) "Paused" else text

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(displayText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(toggleAction)
            .addAction(stopAction)
            .apply {
                launchIntent?.let { setContentIntent(it) }
            }
            .build()
    }
}
