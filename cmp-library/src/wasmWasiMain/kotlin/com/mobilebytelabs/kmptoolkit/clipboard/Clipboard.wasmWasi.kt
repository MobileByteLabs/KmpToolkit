package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * WebAssembly WASI implementation of clipboard operations.
 *
 * WASI (WebAssembly System Interface) does not provide clipboard access
 * as it runs in a sandboxed environment without GUI capabilities.
 *
 * All clipboard operations are no-ops on this platform:
 * - [copyToClipboard] always returns `false`
 * - [getFromClipboard] always returns `null`
 * - [hasClipboardText] always returns `false`
 * - [clearClipboard] does nothing
 *
 * This is expected behavior and should be documented in your application
 * if you target WASI environments.
 */

actual fun copyToClipboard(text: String): Boolean {
    // WASI has no clipboard support
    return false
}

actual fun getFromClipboard(): String? {
    // WASI has no clipboard support
    return null
}

actual fun hasClipboardText(): Boolean {
    // WASI has no clipboard support
    return false
}

actual fun clearClipboard() {
    // WASI has no clipboard support - no-op
}
