package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * tvOS implementation of clipboard operations.
 *
 * Note: tvOS does not have clipboard/pasteboard support like iOS.
 * UIPasteboard is not available on tvOS. These are no-op implementations.
 *
 * See: https://developer.apple.com/documentation/uikit/uipasteboard
 * UIPasteboard is only available on iOS and iPadOS, not tvOS.
 */

actual fun copyToClipboard(text: String): Boolean = false

actual fun getFromClipboard(): String? = null

actual fun hasClipboardText(): Boolean = false

actual fun clearClipboard() {
    // No-op: tvOS does not support clipboard operations
}
