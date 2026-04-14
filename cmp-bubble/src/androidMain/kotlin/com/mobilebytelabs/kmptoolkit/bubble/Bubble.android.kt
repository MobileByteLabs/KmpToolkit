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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mobilebytelabs.kmptoolkit.bubble.android.BubbleMetadataBuilder
import com.mobilebytelabs.kmptoolkit.bubble.android.BubbleShortcut
import com.mobilebytelabs.kmptoolkit.bubble.android.OverlayFab
import com.mobilebytelabs.kmptoolkit.bubble.android.OverlayPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "KmpBubble"

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

    override val capability: BubbleCapability
        get() {
            val context = appContext ?: return BubbleCapability.None
            return when {
                OverlayPermission.canDrawOverlays(context) -> BubbleCapability.Overlay
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> BubbleCapability.Bubble
                else -> BubbleCapability.Notification
            }
        }

    override val capabilityReason: String
        get() = when (capability) {
            BubbleCapability.Overlay -> "Floating FAB (Android ${Build.VERSION.SDK_INT}, overlay permission granted)"
            BubbleCapability.Bubble -> "Bubbles API (Android ${Build.VERSION.SDK_INT}, grant overlay for floating FAB)"
            BubbleCapability.Notification -> "Notification only (Android ${Build.VERSION.SDK_INT})"
            BubbleCapability.None -> "No context available"
            else -> "Android ${Build.VERSION.SDK_INT}"
        }

    private var notificationId = notificationBaseId
    private var currentTitle: String? = null
    private var currentMessage: String? = null
    private var currentActions: List<BubbleAction>? = null
    private var overlayFab: OverlayFab? = null

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
        try {
            val context = appContext ?: run {
                Log.w(TAG, "No app context — cannot show bubble")
                return
            }
            currentTitle = title
            currentMessage = message
            currentActions = actions
            notificationId = notificationBaseId++

            // Check notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    Log.w(
                        TAG,
                        "POST_NOTIFICATIONS not granted — bubble won't show. Call requestNotificationPermission() first.",
                    )
                    _state.value = BubbleState.Dismissed(byUser = false)
                    return
                }
            }

            ensureChannel(context)
            val resolvedStyle = if (style == BubbleStyle.Auto) resolveStyle(context) else style

            when (resolvedStyle) {
                BubbleStyle.Floating -> showFloating(context, title, message, actions, onTap)
                else -> showAsNotification(context, title, message, actions, onTap)
            }

            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            Log.w(TAG, "Bubble show failed gracefully: ${e.message}")
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        try {
            val context = appContext ?: return
            ensureChannel(context)

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

            NotificationManagerCompat.from(context).notify(notificationId, notification)
            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            Log.w(TAG, "Bubble showScreen failed: ${e.message}")
        }
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        try {
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

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            _state.value = BubbleState.Showing
        } catch (e: Exception) {
            Log.w(TAG, "Bubble showPersistent failed: ${e.message}")
        }
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        try {
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
                NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bubble update failed: ${e.message}")
        }
    }

    override fun dismiss() {
        try {
            val context = appContext ?: return
            NotificationManagerCompat.from(context).cancel(notificationId)
            overlayFab?.dismiss()
            overlayFab = null
        } catch (e: Exception) {
            Log.w(TAG, "Bubble dismiss failed: ${e.message}")
        }
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    // ── Strategy Router ─────────────────────────────────────────

    private fun resolveStyle(context: Context): BubbleStyle = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> BubbleStyle.Floating
        OverlayPermission.canDrawOverlays(context) -> BubbleStyle.Floating
        else -> BubbleStyle.Notification
    }

    private fun showFloating(
        context: Context,
        title: String,
        message: String,
        actions: List<BubbleAction>,
        onTap: BubbleTapAction,
    ) {
        // Priority: Overlay FAB (actual floating button) > Bubbles API > Notification
        if (OverlayPermission.canDrawOverlays(context)) {
            // ── Overlay FAB — the actual floating button on screen ──
            Log.i(TAG, "Using overlay FAB (API ${Build.VERSION.SDK_INT})")
            showAsOverlayFab(context, title, onTap)
            // Also show notification with actions + bubble metadata
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                showAsBubbleApi(context, title, message, actions, onTap)
            } else {
                showAsNotification(context, title, message, actions, onTap)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ── Bubbles API — system-managed floating (no overlay permission needed) ──
            Log.i(TAG, "Using Bubbles API (API ${Build.VERSION.SDK_INT}), no overlay permission")
            showAsBubbleApi(context, title, message, actions, onTap)
        } else {
            // ── Notification fallback ───────────────────────────
            Log.i(TAG, "Floating not available on API ${Build.VERSION.SDK_INT}, using notification")
            showAsNotification(context, title, message, actions, onTap)
        }
    }

    private fun showAsBubbleApi(
        context: Context,
        title: String,
        message: String,
        actions: List<BubbleAction>,
        onTap: BubbleTapAction,
    ) {
        // Create shortcut (required for Bubbles)
        val shortcutId = BubbleShortcut.getOrCreateShortcut(context, title)

        // Build bubble metadata
        val actionsStr = actions.joinToString(",") { it.label }
        val bubbleMetadata = BubbleMetadataBuilder.build(
            context = context,
            title = title,
            message = message,
            actions = actionsStr,
        )

        // Build notification with bubble
        val builder = NotificationCompat.Builder(context, config.channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(bubbleMetadata)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)

        if (config.vibrate) builder.setVibrate(longArrayOf(0, 250))
        if (!config.sound) builder.setSilent(true)

        // Tap action on notification (not bubble)
        applyTapAction(context, builder, onTap)

        // Action buttons
        applyActionButtons(context, builder, actions)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun showAsOverlayFab(context: Context, title: String, onTap: BubbleTapAction) {
        overlayFab?.dismiss()
        overlayFab = OverlayFab(context).apply {
            this.onTap = {
                when (onTap) {
                    is BubbleTapAction.DeepLink -> {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onTap.uri)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }

                    is BubbleTapAction.Callback -> onTap.onTap()

                    is BubbleTapAction.Dismiss -> dismiss()

                    else -> {}
                }
            }
            show(title)
        }
    }

    private fun showAsNotification(
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

        applyTapAction(context, builder, onTap)
        applyActionButtons(context, builder, actions)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun applyTapAction(context: Context, builder: NotificationCompat.Builder, onTap: BubbleTapAction) {
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

            else -> {}
        }
    }

    private fun applyActionButtons(context: Context, builder: NotificationCompat.Builder, actions: List<BubbleAction>) {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        actions.forEachIndexed { index, action ->
            val actionIntent = Intent("com.mobilebytelabs.kmptoolkit.bubble.ACTION_${action.id}")
            val actionPending = PendingIntent.getBroadcast(context, index + 100, actionIntent, flags)
            builder.addAction(NotificationCompat.Action.Builder(0, action.label, actionPending).build())
        }
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
