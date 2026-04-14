package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Permission checking for bubble/notification capabilities.
 *
 * Permissions vary by platform:
 *
 * | Platform | Bubble Permission | Notification Permission |
 * |----------|------------------|------------------------|
 * | Android 30+ | Auto-granted (Bubbles API) | POST_NOTIFICATIONS (API 33+) |
 * | Android <30 | SYSTEM_ALERT_WINDOW | Auto-granted |
 * | iOS | N/A | UNUserNotificationCenter authorization |
 * | macOS | Auto-granted | Auto-granted |
 * | JVM | Auto-granted | Auto-granted |
 * | JS/Wasm | N/A | Notification.permission |
 *
 * @since 0.1.0
 */
interface BubblePermission {
    /** Can show floating bubbles (Android: Bubbles API or SYSTEM_ALERT_WINDOW). */
    fun canShowBubble(): Boolean

    /** Can show notifications (Android 13+: POST_NOTIFICATIONS, iOS: UNAuth). */
    fun canShowNotification(): Boolean

    /** Request permission to show floating bubbles. Returns true if granted. */
    suspend fun requestBubblePermission(): Boolean

    /** Request permission to show notifications. Returns true if granted. */
    suspend fun requestNotificationPermission(): Boolean
}

/**
 * Creates a platform-specific [BubblePermission] instance.
 *
 * @since 0.1.0
 */
expect fun createBubblePermission(): BubblePermission
