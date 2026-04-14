package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * Platform-specific clipboard and overlay permission checking.
 *
 * Permissions vary significantly by platform:
 *
 * - **Android**: `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` (API 33+), `SYSTEM_ALERT_WINDOW`
 * - **iOS**: `UIPasteboard` access shows system paste banner (iOS 16+)
 * - **macOS/JVM/Desktop**: Generally no permissions needed
 * - **JS/Wasm**: Clipboard API requires user gesture + Permissions API
 * - **tvOS/watchOS/WASI**: Not applicable
 *
 * ## Usage
 *
 * ```kotlin
 * val permission = createClipboardPermission()
 *
 * if (!permission.hasClipboardAccess()) {
 *     val granted = permission.requestClipboardAccess()
 *     if (!granted) {
 *         println("Clipboard access denied")
 *         return
 *     }
 * }
 *
 * if (config.showOverlay && !permission.hasOverlayPermission()) {
 *     permission.requestOverlayPermission()
 * }
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardPermission {
    /** Whether the app has clipboard read/write access. */
    fun hasClipboardAccess(): Boolean

    /** Whether the app can show floating overlays (Android: SYSTEM_ALERT_WINDOW). */
    fun hasOverlayPermission(): Boolean

    /** Whether the app can show notifications (Android: POST_NOTIFICATIONS). */
    fun hasNotificationPermission(): Boolean

    /** Request clipboard access. Returns true if granted. */
    suspend fun requestClipboardAccess(): Boolean

    /** Request overlay permission. Returns true if granted. */
    suspend fun requestOverlayPermission(): Boolean

    /** Request notification permission. Returns true if granted. */
    suspend fun requestNotificationPermission(): Boolean
}

/**
 * Creates a platform-specific [ClipboardPermission] instance.
 *
 * @since 0.2.0
 */
expect fun createClipboardPermission(): ClipboardPermission
