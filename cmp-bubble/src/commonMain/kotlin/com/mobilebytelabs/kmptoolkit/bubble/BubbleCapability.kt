package com.mobilebytelabs.kmptoolkit.bubble

/**
 * What floating UI capability this platform actually supports.
 *
 * Use [Bubble.capability] to check before showing, or just call [Bubble.show]
 * which gracefully degrades and never crashes.
 *
 * @since 0.1.0
 */
enum class BubbleCapability {
    /** Android 30+ Bubbles API — real floating chat-head with embedded Activity. */
    Bubble,

    /** Android <30 TYPE_APPLICATION_OVERLAY — floating FAB window. */
    Overlay,

    /** Standard platform notification (iOS banner, Android notification, etc.). */
    Notification,

    /** Desktop floating window (macOS NSPanel, JVM JWindow). */
    FloatingWindow,

    /** Browser Notification API (JS/Wasm). */
    BrowserNotification,

    /** Platform has no notification or floating capability. */
    None,
}
