package com.mobilebytelabs.kmptoolkit.clipboard.overlay

/**
 * Configuration for the clipboard monitor floating overlay (FAB).
 *
 * Controls the appearance and behavior of the floating action button
 * that appears when a URL is detected in the clipboard.
 *
 * ## Platform Support
 *
 * | Platform | Overlay Type |
 * |----------|-------------|
 * | Android  | TYPE_APPLICATION_OVERLAY floating FAB |
 * | macOS    | NSStatusItem menu bar + NSPanel |
 * | JVM      | System tray icon + JWindow |
 * | Windows  | System tray + floating window |
 * | iOS      | Local notification banner (no system overlay) |
 * | JS/Wasm  | Fixed-position DOM element |
 * | Others   | Not supported |
 *
 * @property position Initial position of the FAB on screen.
 * @property draggable Whether the FAB can be dragged by the user.
 * @property autoDismissMs Auto-dismiss the overlay after this many ms (0 = no auto-dismiss).
 * @property showOnUrlOnly Only show overlay when a URL is detected (not on every change).
 * @property vibrate Whether to vibrate on URL detection (mobile only).
 * @since 0.2.0
 */
data class ClipboardOverlayConfig(
    val position: OverlayPosition = OverlayPosition.BottomEnd,
    val draggable: Boolean = true,
    val autoDismissMs: Long = 0L,
    val showOnUrlOnly: Boolean = true,
    val vibrate: Boolean = true,
) {
    companion object {
        val Default = ClipboardOverlayConfig()
    }
}

/**
 * Position of the floating overlay on screen.
 *
 * @since 0.2.0
 */
enum class OverlayPosition {
    TopStart,
    TopEnd,
    BottomStart,
    BottomEnd,
    CenterEnd,
}
