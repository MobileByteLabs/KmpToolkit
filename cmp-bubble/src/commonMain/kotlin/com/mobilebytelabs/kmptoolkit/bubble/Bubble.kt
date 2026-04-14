package com.mobilebytelabs.kmptoolkit.bubble

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform floating UI and notification system.
 *
 * Shows bubbles, notifications, and floating overlays using the best native
 * mechanism on each platform:
 *
 * - **Android 30+**: Bubbles API (no special permission needed)
 * - **Android <30**: TYPE_APPLICATION_OVERLAY floating FAB
 * - **iOS**: Local notification banners with action buttons
 * - **macOS**: NSUserNotificationCenter
 * - **JVM**: System tray notifications
 * - **JS/Wasm**: Browser Notification API
 *
 * ## Quick Start
 *
 * ```kotlin
 * val bubble = createBubble()
 *
 * bubble.show(
 *     title = "Download Complete",
 *     message = "video.mp4 saved",
 *     actions = listOf(
 *         BubbleAction("Open") { openFile() },
 *         BubbleAction("Share") { shareFile() }
 *     )
 * )
 * ```
 *
 * ## Open a Screen
 *
 * ```kotlin
 * bubble.showScreen(
 *     title = "Quick Reply",
 *     route = "chat/reply/$messageId",
 *     screenConfig = BubbleScreenConfig(height = 400)
 * )
 * ```
 *
 * @since 0.1.0
 */
interface Bubble {
    /** Current bubble state. */
    val state: StateFlow<BubbleState>

    /** Whether the bubble is currently visible. */
    val isShowing: Boolean

    /**
     * What floating UI capability this platform supports.
     *
     * Check this to know what will happen when you call [show] with [BubbleStyle.Floating]:
     * - [BubbleCapability.Bubble] — Android 30+ real floating bubble
     * - [BubbleCapability.Overlay] — Android <30 floating FAB
     * - [BubbleCapability.FloatingWindow] — macOS/JVM floating window
     * - [BubbleCapability.Notification] — notification only (no floating)
     * - [BubbleCapability.None] — nothing available
     */
    val capability: BubbleCapability

    /** Human-readable reason for the current [capability]. */
    val capabilityReason: String

    /**
     * Show a bubble with content.
     *
     * @param title Primary text / heading.
     * @param message Secondary text / body.
     * @param icon Optional icon.
     * @param actions Action buttons (platform max varies: Android unlimited, iOS 3, JS 2).
     * @param style Visual style and behavior.
     * @param onTap What happens when the bubble body is tapped.
     * @param autoDismissMs Auto-dismiss after this many milliseconds (0 = no auto-dismiss).
     */
    fun show(
        title: String,
        message: String = "",
        icon: BubbleIcon? = null,
        actions: List<BubbleAction> = emptyList(),
        style: BubbleStyle = BubbleStyle.Auto,
        onTap: BubbleTapAction = BubbleTapAction.None,
        autoDismissMs: Long = 0L,
    )

    /**
     * Show a screen inside the bubble.
     *
     * On Android 30+, this opens an Activity inside the Bubble chat-head.
     * On iOS, the route is passed as a deep link when the notification is tapped.
     * On other platforms, the route is passed to the tap callback.
     *
     * @param title Bubble title.
     * @param route Deep link route / URI to open.
     * @param screenConfig Screen dimensions and behavior.
     * @param icon Optional icon.
     * @param style Visual style.
     */
    fun showScreen(
        title: String,
        route: String,
        screenConfig: BubbleScreenConfig = BubbleScreenConfig.Default,
        icon: BubbleIcon? = null,
        style: BubbleStyle = BubbleStyle.Floating,
    )

    /**
     * Show a persistent bubble that stays until explicitly dismissed.
     *
     * On Android, this creates an ongoing notification (or foreground service notification
     * if style is [BubbleStyle.Service]).
     *
     * @param title Bubble title.
     * @param message Bubble message.
     * @param actions Action buttons (e.g., Pause, Stop).
     * @param style [BubbleStyle.Persistent] or [BubbleStyle.Service].
     */
    fun showPersistent(
        title: String,
        message: String = "",
        actions: List<BubbleAction> = emptyList(),
        style: BubbleStyle = BubbleStyle.Persistent,
    )

    /**
     * Update the content of the currently shown bubble.
     *
     * Pass null for any parameter to keep the current value.
     */
    fun update(title: String? = null, message: String? = null, actions: List<BubbleAction>? = null)

    /** Dismiss the current bubble. */
    fun dismiss()
}

/**
 * Creates a platform-specific [Bubble] instance.
 *
 * @param config Global bubble configuration.
 * @return A new Bubble ready to use.
 * @since 0.1.0
 */
expect fun createBubble(config: BubbleConfig = BubbleConfig.Default): Bubble
