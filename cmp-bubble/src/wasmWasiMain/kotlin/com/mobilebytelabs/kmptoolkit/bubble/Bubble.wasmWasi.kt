package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class WasmWasiBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = false
    override val capability: BubbleCapability
        get() = if (WasiBubbleHost.isConfigured) BubbleCapability.Notification else BubbleCapability.None
    override val capabilityReason: String
        get() = if (WasiBubbleHost.isConfigured) {
            "Delivered to the WASI host via WasiBubbleHost.onNotify"
        } else {
            "WASI is headless; set WasiBubbleHost.onNotify to receive notifications, " +
                "or leave it unset to have them echoed to stdout"
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
        if (WasiBubbleHost.deliver(title, message)) {
            _state.value = BubbleState.Showing
        }
    }

    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {
        // Nothing to embed a screen in; naming the route is the honest degradation.
        show(title = title, message = "Open: $route", style = style)
    }

    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {
        show(title = title, message = message, style = style)
    }

    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {
        if (title != null) show(title = title, message = message ?: "")
    }

    override fun dismiss() {
        _state.value = BubbleState.Dismissed(byUser = false)
    }
}

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(WasmWasiBubble(config))
