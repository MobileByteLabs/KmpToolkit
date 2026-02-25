package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

/**
 * JavaScript browser implementation of clipboard operations.
 *
 * This implementation uses the modern Clipboard API (navigator.clipboard)
 * with a fallback to the deprecated execCommand for older browsers.
 *
 * ## Limitations
 *
 * - The Clipboard API is asynchronous, but this interface is synchronous.
 *   Copy operations are "fire-and-forget" - they return immediately.
 * - Reading from clipboard requires user permission and is async in JS.
 *   [getFromClipboard] returns null on this platform. Use coroutines for async read.
 * - [hasClipboardText] always returns false due to async API limitations.
 *
 * ## Browser Support
 *
 * - Modern browsers (Chrome 66+, Firefox 63+, Safari 13.1+): Full write support
 * - Older browsers: Falls back to execCommand (deprecated but widely supported)
 */

actual fun copyToClipboard(text: String): Boolean {
    return try {
        // Try modern Clipboard API first (async, fire-and-forget)
        val clipboard = window.navigator.asDynamic().clipboard
        if (clipboard != null) {
            clipboard.writeText(text)
            true
        } else {
            // Fallback to execCommand for older browsers
            copyWithExecCommand(text)
        }
    } catch (e: Exception) {
        // Fallback to execCommand if Clipboard API fails
        try {
            copyWithExecCommand(text)
        } catch (e2: Exception) {
            false
        }
    }
}

/**
 * Fallback copy method using deprecated execCommand.
 * Works in older browsers and some restricted contexts.
 */
private fun copyWithExecCommand(text: String): Boolean {
    return try {
        val textArea = document.createElement("textarea") as HTMLTextAreaElement
        textArea.value = text
        textArea.style.position = "fixed"
        textArea.style.left = "-9999px"
        textArea.style.top = "-9999px"
        document.body?.appendChild(textArea)
        textArea.select()
        val result = document.execCommand("copy")
        document.body?.removeChild(textArea)
        result
    } catch (e: Exception) {
        false
    }
}

actual fun getFromClipboard(): String? {
    // Reading from clipboard in JS requires async Clipboard API
    // and user permission. Cannot implement synchronously.
    // Users should use suspend version for clipboard reading.
    return null
}

actual fun hasClipboardText(): Boolean {
    // Cannot determine clipboard state synchronously in JavaScript
    return false
}

actual fun clearClipboard() {
    // Clear by copying empty string
    copyToClipboard("")
}
