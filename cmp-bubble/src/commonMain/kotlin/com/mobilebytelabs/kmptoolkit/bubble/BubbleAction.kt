package com.mobilebytelabs.kmptoolkit.bubble

/**
 * An action button displayed on a bubble or notification.
 *
 * Maps to:
 * - Android: `NotificationCompat.Action` or Bubble Activity button
 * - iOS: `UNNotificationAction`
 * - JS: `NotificationAction`
 * - Desktop: Tray menu item
 *
 * ## Usage
 *
 * ```kotlin
 * bubble.show(
 *     title = "URL Detected",
 *     message = "https://instagram.com/reel/123",
 *     actions = listOf(
 *         BubbleAction("Download") { startDownload(url) },
 *         BubbleAction("Open") { openBrowser(url) },
 *         BubbleAction("Dismiss")
 *     )
 * )
 * ```
 *
 * @property label Display text for the button.
 * @property id Unique identifier, used in [BubbleState.ActionTaken]. Defaults to lowercased label.
 * @property onClick Optional callback when the action is tapped.
 * @since 0.1.0
 */
data class BubbleAction(
    val label: String,
    val id: String = label.lowercase().replace(" ", "_"),
    val onClick: (() -> Unit)? = null,
)
