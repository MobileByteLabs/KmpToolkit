package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * WebAssembly JavaScript implementation of clipboard operations.
 *
 * Note: The browser Clipboard API requires proper DOM interop and permissions
 * which are complex to set up in Kotlin/Wasm. These are no-op implementations.
 *
 * For clipboard functionality in Wasm/JS environments, consider using
 * JavaScript interop directly in your application code.
 */

actual fun copyToClipboard(text: String): Boolean = false

actual fun getFromClipboard(): String? = null

actual fun hasClipboardText(): Boolean = false

actual fun clearClipboard() {
    // No-op: Wasm clipboard requires complex JS interop setup
}
