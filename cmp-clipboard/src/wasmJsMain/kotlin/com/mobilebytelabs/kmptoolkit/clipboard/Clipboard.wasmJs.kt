package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * WebAssembly JavaScript implementation of clipboard operations.
 *
 * Uses Kotlin/Wasm JS interop to access the browser Clipboard API.
 *
 * ## Limitations
 *
 * - Write (copy) works via navigator.clipboard.writeText (fire-and-forget)
 * - Synchronous read is NOT possible — use [getFromClipboardAsync] instead
 * - [hasClipboardText] always returns false (async-only check)
 *
 * For full clipboard read support, use the suspend functions in ClipboardAsync.
 */

@JsFun("(text) => { try { navigator.clipboard.writeText(text); return true; } catch(e) { return false; } }")
private external fun jsWriteClipboard(text: String): Boolean

actual fun copyToClipboard(text: String): Boolean = try {
    jsWriteClipboard(text)
} catch (e: Throwable) {
    false
}

actual fun getFromClipboard(): String? {
    // Synchronous clipboard read is not possible in browser environments.
    // Use getFromClipboardAsync() for async clipboard reading.
    return null
}

actual fun hasClipboardText(): Boolean {
    // Cannot determine synchronously in Wasm JS
    return false
}

actual fun clearClipboard() {
    copyToClipboard("")
}
