package com.mobilebytelabs.kmptoolkit.clipboard

import platform.AppKit.NSPasteboard
import platform.AppKit.NSPasteboardTypeString

/**
 * macOS implementation of clipboard operations using NSPasteboard.
 *
 * This implementation provides full clipboard support on macOS
 * using the system's general pasteboard.
 */

actual fun copyToClipboard(text: String): Boolean = try {
    val pasteboard = NSPasteboard.generalPasteboard
    pasteboard.clearContents()
    pasteboard.setString(text, forType = NSPasteboardTypeString)
    true
} catch (e: Exception) {
    false
}

actual fun getFromClipboard(): String? = try {
    NSPasteboard.generalPasteboard.stringForType(NSPasteboardTypeString)
} catch (e: Exception) {
    null
}

actual fun hasClipboardText(): Boolean = try {
    NSPasteboard.generalPasteboard.stringForType(NSPasteboardTypeString) != null
} catch (e: Exception) {
    false
}

actual fun clearClipboard() {
    try {
        NSPasteboard.generalPasteboard.clearContents()
    } catch (e: Exception) {
        // Silently ignore errors
    }
}
