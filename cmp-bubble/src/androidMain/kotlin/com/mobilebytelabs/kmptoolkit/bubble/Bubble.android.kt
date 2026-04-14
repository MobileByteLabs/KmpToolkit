package com.mobilebytelabs.kmptoolkit.bubble

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("StaticFieldLeak")
internal var appContext: Context? = null
    private set

internal fun setApplicationContext(context: Context) {
    appContext = context.applicationContext
}

class BubbleInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { setApplicationContext(it) }
        return true
    }
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}

internal class AndroidBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    private var notificationId = notificationBaseId
    private var currentTitle: String? = null
    private var currentMessage: String? = null
    private var currentActions: List<BubbleAction>? = null

    companion object {
        private var notificationBaseId = 9950
    }

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        val context = appContext ?: return
        currentTitle = title
        currentMessage = message
        currentActions = actions
        notificationId = notificationBaseId++

        ensureChannel(context)

        val resolvedStyle = if (style == BubbleStyle.Auto) resolveStyle() else style

        when (resolvedStyle) {
            BubbleStyle.Floating -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    showBubbleNotification(context, title, message, actions, onTap)
                } else {
                    showStandardNotification(context, title, message, actions, onTap)
                }
            }

            else -> showStandardNotification(context, title, message, actions, onTap)
        }

        _state.value = BubbleState.Showing
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        val context = appContext ?: return
        ensureChannel(context)

        // Create deep link intent
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(route)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) { /* missing POST_NOTIFICATIONS permission */ }

        _state.value = BubbleState.Showing
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        val context = appContext ?: return
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        actions.forEachIndexed { index, action ->
            val actionIntent = Intent("com.mobilebytelabs.kmptoolkit.bubble.ACTION_${action.id}")
            val actionPending = PendingIntent.getBroadcast(context, index + 100, actionIntent, flags)
            builder.addAction(NotificationCompat.Action.Builder(0, action.label, actionPending).build())
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) { /* missing permission */ }

        _state.value = BubbleState.Showing
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        title?.let { currentTitle = it }
        message?.let { currentMessage = it }
        actions?.let { currentActions = it }

        if (_state.value is BubbleState.Showing) {
            val context = appContext ?: return
            val builder = NotificationCompat.Builder(context, config.channelId)
                .setContentTitle(currentTitle)
                .setContentText(currentMessage)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

            try {
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            } catch (e: SecurityException) { /* missing permission */ }
        }
    }

    override fun dismiss() {
        val context = appContext ?: return
        NotificationManagerCompat.from(context).cancel(notificationId)
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    private fun resolveStyle(): BubbleStyle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BubbleStyle.Floating
    } else {
        BubbleStyle.Notification
    }

    private fun showBubbleNotification(
        context: Context,
        title: String,
        message: String,
        actions: List<BubbleAction>,
        onTap: BubbleTapAction,
    ) {
        // On API 30+, create a bubble-style notification
        // Full BubbleMetadata + ShortcutInfo will be added when BubbleActivity is ready
        // For now, use a high-priority notification that looks similar
        showStandardNotification(context, title, message, actions, onTap)
    }

    private fun showStandardNotification(
        context: Context,
        title: String,
        message: String,
        actions: List<BubbleAction>,
        onTap: BubbleTapAction,
    ) {
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (config.vibrate) builder.setVibrate(longArrayOf(0, 250))
        if (!config.sound) builder.setSilent(true)

        // Tap action
        when (onTap) {
            is BubbleTapAction.DeepLink -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onTap.uri)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pending = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                builder.setContentIntent(pending)
            }

            else -> { /* No content intent */ }
        }

        // Action buttons
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        actions.forEachIndexed { index, action ->
            val actionIntent = Intent("com.mobilebytelabs.kmptoolkit.bubble.ACTION_${action.id}")
            val actionPending = PendingIntent.getBroadcast(context, index + 100, actionIntent, flags)
            builder.addAction(NotificationCompat.Action.Builder(0, action.label, actionPending).build())
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) { /* missing POST_NOTIFICATIONS permission */ }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(config.channelId, config.channelName, importance).apply {
                description = "Bubble notifications"
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = AndroidBubble(config)
