package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLTextAreaElement

/**
 * WebAssembly JavaScript implementation of clipboard operations.
 *
 * This implementation is nearly identical to the JS implementation,
 * using the modern Clipboard API with execCommand fallback.
 *
 * ## Limitations
 *
 * Same limitations as JS implementation:
 * - Copy operations are "fire-and-forget"
 * - [getFromClipboard] returns null
 * - [hasClipboardText] always returns false
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
    // Reading from clipboard requires async API in browser
    return null
}

actual fun hasClipboardText(): Boolean {
    // Cannot determine synchronously
    return false
}

actual fun clearClipboard() {
    copyToClipboard("")
}
