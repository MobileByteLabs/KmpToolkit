package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Visual style and behavior of a bubble.
 *
 * The platform selects the best native mechanism for each style:
 *
 * | Style | Android 30+ | Android <30 | iOS | Desktop |
 * |-------|------------|------------|-----|---------|
 * | Floating | Bubble API | Overlay FAB | N/A (Notification) | System tray |
 * | Notification | Notification | Notification | Banner | Tray popup |
 * | Persistent | Ongoing notification | Ongoing overlay | Ongoing notif | Tray icon |
 * | Service | Foreground service notif | Foreground service notif | N/A | N/A |
 * | Auto | Platform decides | Platform decides | Platform decides | Platform decides |
 *
 * @since 0.1.0
 */
enum class BubbleStyle {
    /** Floating chat-head style (Android Bubble / overlay FAB). */
    Floating,

    /** Standard notification banner (iOS banner, Android notification, browser Notification API). */
    Notification,

    /** Persistent — stays until explicitly dismissed. */
    Persistent,

    /** Service notification — for long-running background tasks (Android foreground service). */
    Service,

    /** Auto — platform picks the best available style. */
    Auto,
}
