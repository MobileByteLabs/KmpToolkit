package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * watchOS implementation of clipboard operations.
 *
 * Note: watchOS does not have clipboard/pasteboard support like iOS.
 * UIPasteboard is not available on watchOS. These are no-op implementations.
 *
 * See: https://developer.apple.com/documentation/uikit/uipasteboard
 * UIPasteboard is only available on iOS and iPadOS, not watchOS.
 */

actual fun copyToClipboard(text: String): Boolean = false

actual fun getFromClipboard(): String? = null

actual fun hasClipboardText(): Boolean = false

actual fun clearClipboard() {
    // No-op: watchOS does not support clipboard operations
}
