package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class TvosBubble(private val config: BubbleConfig) : Bubble {
    private val _state = MutableStateFlow<BubbleState>(BubbleState.Hidden)
    override val state: StateFlow<BubbleState> = _state.asStateFlow()
    override val isShowing: Boolean get() = false
    override val capability: BubbleCapability = BubbleCapability.None
    override val capabilityReason: String =
        "tvOS notifications are badge-only: UNUserNotificationCenter exists, but " +
            "UNMutableNotificationContent exposes no title or body on this platform, so there " +
            "is no user-visible alert to raise (verified by compiling against it)"

    override fun show(
        title: String,
        message: String,
        icon: BubbleIcon?,
        actions: List<BubbleAction>,
        style: BubbleStyle,
        onTap: BubbleTapAction,
        autoDismissMs: Long,
    ) {
    }
    override fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig,
        icon: BubbleIcon?,
        style: BubbleStyle,
    ) {}
    override fun showPersistent(title: String, message: String, actions: List<BubbleAction>, style: BubbleStyle) {}
    override fun update(title: String?, message: String?, actions: List<BubbleAction>?) {}
    override fun dismiss() {}
}

actual fun createBubble(config: BubbleConfig): Bubble = reportBubbleCreated(TvosBubble(config))
