@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@JsFun("(title, body) => { try { new Notification(title, {body: body}); return true; } catch(e) { return false; } }")
internal external fun jsShowNotification(title: String, body: String): Boolean

@JsFun("() => { try { return Notification.permission === 'granted'; } catch(e) { return false; } }")
internal external fun jsHasNotificationPermission(): Boolean

@JsFun("() => { try { Notification.requestPermission(); return true; } catch(e) { return false; } }")
internal external fun jsRequestNotificationPermission(): Boolean

internal class WasmJsBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = _state.value is BubbleState.Showing
    override val capability: BubbleCapability = BubbleCapability.BrowserNotification
    override val capabilityReason: String = "Wasm JS Notification API"

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
        if (jsShowNotification(title, message)) {
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

actual fun createBubble(config: BubbleConfig): Bubble = WasmJsBubble(config)
