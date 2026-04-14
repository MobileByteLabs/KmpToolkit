package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class JsBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing

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
            val permission = js("Notification.permission") as? String
            if (permission == "granted") {
                val options = js("{}")
                options.body = message
                if (icon is BubbleIcon.Url) options.icon = icon.url
                js("new Notification(title, options)")
                _state.value = BubbleState.Showing
            } else if (permission == "default") {
                js("Notification.requestPermission()")
            }
        } catch (e: Throwable) { /* Notification API not available */ }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        // Browser notifications can't be updated — show new one
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = JsBubble(config)
