package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * Asynchronous clipboard operations.
 *
 * Provides suspend function variants of the basic clipboard operations.
 * Essential for platforms where clipboard access is inherently async
 * (JS/Wasm Clipboard API), and useful everywhere for non-blocking I/O.
 *
 * ## Usage
 *
 * ```kotlin
 * // In a coroutine
 * val text = getFromClipboardAsync()
 * text?.let { println("Clipboard: $it") }
 *
 * val success = copyToClipboardAsync("Hello!")
 * ```
 *
 * ## Platform Behavior
 *
 * | Platform | Behavior |
 * |----------|----------|
 * | Android  | Dispatches to Main thread |
 * | iOS/macOS| Dispatches to Main thread |
 * | JVM      | Dispatches to IO thread |
 * | JS/Wasm  | Uses navigator.clipboard async API |
 * | Linux    | Dispatches xclip to IO thread |
 * | Windows  | Dispatches to IO thread |
 *
 * @since 0.2.0
 */

/**
 * Reads text from the clipboard asynchronously.
 *
 * Unlike [getFromClipboard], this function works on all platforms including
 * JS/Wasm where clipboard access is inherently asynchronous.
 *
 * @return The clipboard text content, or null if empty/unavailable.
 */
expect suspend fun getFromClipboardAsync(): String?

/**
 * Copies text to the clipboard asynchronously.
 *
 * @param text The text to copy.
 * @return true if the operation succeeded.
 */
expect suspend fun copyToClipboardAsync(text: String): Boolean

/**
 * Checks if the clipboard contains text asynchronously.
 *
 * @return true if the clipboard has text content.
 */
expect suspend fun hasClipboardTextAsync(): Boolean
