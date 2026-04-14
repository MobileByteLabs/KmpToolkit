package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUUID
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

internal class MacosBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

    private var currentNotificationId: String? = null

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long
    ) {
        val notificationId = NSUUID().UUIDString
        currentNotificationId = notificationId

        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            if (config.sound) {
                setSound(UNNotificationSound.defaultSound)
            }
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notificationId,
            content = content,
            trigger = null
        )

        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            if (error == null) {
                _state.value = BubbleState.Showing
            }
        }
    }

    override fun showScreen(title: String, route: String, screenConfig: BubbleScreenConfig, icon: BubbleIcon?, style: BubbleStyle) {
        show(title = title, message = route, icon = icon, style = style, onTap = BubbleTapAction.DeepLink(route))
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, actions = actions, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        val id = currentNotificationId ?: return
        val content = UNMutableNotificationContent().apply {
            title?.let { setTitle(it) }
            message?.let { setBody(it) }
        }
        val request = UNNotificationRequest.requestWithIdentifier(identifier = id, content = content, trigger = null)
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    override fun dismiss() {
        currentNotificationId?.let { id ->
            UNUserNotificationCenter.currentNotificationCenter().removeDeliveredNotificationsWithIdentifiers(listOf(id))
        }
        currentNotificationId = null
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = MacosBubble(config)
