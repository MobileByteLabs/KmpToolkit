package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSBundle
import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/** Check if notification APIs are available (requires app bundle — absent in test runners). */
private fun isNotificationAvailable(): Boolean = NSBundle.mainBundle.bundleIdentifier != null

internal class IosBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    private var currentNotificationId: String? = null

    companion object {
        private const val CATEGORY_ID = "KMPTOOLKIT_BUBBLE"
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
        // Guard: UNUserNotificationCenter crashes without app bundle (e.g., in unit tests)
        if (!isNotificationAvailable()) return

        val notificationId = NSUUID().UUIDString
        currentNotificationId = notificationId

        if (actions.isNotEmpty()) {
            registerCategory(actions)
        }

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            if (actions.isNotEmpty()) {
                setCategoryIdentifier(CATEGORY_ID)
            }
            if (config.sound) {
                setSound(UNNotificationSound.defaultSound)
            }
            when (onTap) {
                is BubbleTapAction.DeepLink -> {
                    setUserInfo(mapOf("route" to onTap.uri))
                }

                else -> {}
            }
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notificationId,
            content = content,
            trigger = null,
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error == null) {
                _state.value = BubbleState.Showing
            }
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        // On iOS, showing a "screen" means posting a notification
        // that deep-links to the route when tapped
        show(
            title = title,
            message = route,
            icon = icon,
            style = style,
            onTap = BubbleTapAction.DeepLink(route),
        )
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        // iOS doesn't have persistent notifications in the same way as Android
        // Show a standard notification that stays in notification center
        show(title = title, message = message, actions = actions, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (!isNotificationAvailable()) return
        val id = currentNotificationId ?: return
        // Re-post notification with same ID to update
        val content = UNMutableNotificationContent().apply {
            title?.let { setTitle(it) }
            message?.let { setBody(it) }
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = null,
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    override fun dismiss() {
        if (isNotificationAvailable()) {
            currentNotificationId?.let { id ->
                UNUserNotificationCenter.currentNotificationCenter()
                    .removeDeliveredNotificationsWithIdentifiers(listOf(id))
            }
        }
        currentNotificationId = null
        _state.value = BubbleState.Dismissed(byUser = false)
    }

    private fun registerCategory(actions: List<BubbleAction>) {
        val notificationActions = actions.map { action ->
            UNNotificationAction.actionWithIdentifier(
                identifier = action.id,
                title = action.label,
                options = UNNotificationActionOptionNone,
            )
        }

        val category = UNNotificationCategory.categoryWithIdentifier(
            identifier = CATEGORY_ID,
            actions = notificationActions,
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionNone,
        )

        UNUserNotificationCenter.currentNotificationCenter()
            .setNotificationCategories(setOf(category))
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = IosBubble(config)
