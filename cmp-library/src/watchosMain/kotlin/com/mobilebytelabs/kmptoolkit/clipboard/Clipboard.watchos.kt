package com.mobilebytelabs.kmptoolkit.clipboard

import platform.UIKit.UIPasteboard

/**
 * watchOS implementation of clipboard operations using UIPasteboard.
 *
 * This implementation provides full clipboard support on watchOS devices
 * using the system's general pasteboard.
 */

actual fun copyToClipboard(text: String): Boolean = try {
    UIPasteboard.generalPasteboard.string = text
    true
} catch (e: Exception) {
    false
}

actual fun getFromClipboard(): String? = try {
    UIPasteboard.generalPasteboard.string
} catch (e: Exception) {
    null
}

actual fun hasClipboardText(): Boolean = try {
    UIPasteboard.generalPasteboard.hasStrings
} catch (e: Exception) {
    false
}

actual fun clearClipboard() {
    try {
        UIPasteboard.generalPasteboard.string = ""
    } catch (e: Exception) {
        // Silently ignore errors
    }
}
