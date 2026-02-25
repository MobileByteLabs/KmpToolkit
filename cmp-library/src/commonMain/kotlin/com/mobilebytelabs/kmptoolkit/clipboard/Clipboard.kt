package com.mobilebytelabs.kmptoolkit.clipboard

/*
 * Cross-platform clipboard utilities for Kotlin Multiplatform.
 *
 * Provides simple functions to interact with the system clipboard across
 * all supported platforms: Android, iOS, macOS, JVM, JS, Linux, and Windows.
 *
 * ## Zero Configuration
 *
 * No setup required! Just import and use:
 *
 * ```kotlin
 * import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
 * import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
 *
 * // Copy text - works immediately on all platforms
 * copyToClipboard("Hello, World!")
 *
 * // Read text (where supported)
 * val text = getFromClipboard()
 * ```
 *
 * ## Platform Support
 *
 * | Platform | Copy | Read | Notes |
 * |----------|:----:|:----:|-------|
 * | Android  | ✅   | ✅   | Auto-initialized via ContentProvider |
 * | iOS      | ✅   | ✅   | Full support |
 * | macOS    | ✅   | ✅   | Full support |
 * | tvOS     | ✅   | ✅   | Full support |
 * | watchOS  | ✅   | ✅   | Full support |
 * | JVM      | ✅   | ✅   | Uses AWT Toolkit |
 * | JS       | ✅   | ❌   | Async API, write-only for sync |
 * | Wasm JS  | ✅   | ❌   | Async API, write-only for sync |
 * | Linux    | ✅   | ✅   | Requires xclip or xsel |
 * | Windows  | ✅   | ✅   | Uses Win32 API |
 * | WASI     | ❌   | ❌   | No clipboard in WASI runtime |
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Copy text to clipboard
 * val success = copyToClipboard("Hello, World!")
 *
 * // Read text from clipboard (where supported)
 * val text = getFromClipboard()
 *
 * // Check if clipboard has text
 * if (hasClipboardText()) {
 *     println("Clipboard has content")
 * }
 *
 * // Clear clipboard
 * clearClipboard()
 * ```
 *
 * @since 0.1.0
 */

/**
 * Copies the given text to the system clipboard.
 *
 * This function attempts to copy the provided text to the system clipboard.
 * The operation is synchronous on most platforms, but may be fire-and-forget
 * on JavaScript platforms due to async clipboard API limitations.
 *
 * @param text The text to copy to clipboard. Can be any valid string.
 * @return `true` if the copy operation was initiated successfully, `false` otherwise.
 *         Note: On JS platforms, this returns `true` when the operation is initiated,
 *         not when it completes.
 * @since 0.1.0
 *
 * @sample
 * ```kotlin
 * val success = copyToClipboard("Hello, Clipboard!")
 * if (success) {
 *     println("Text copied successfully")
 * }
 * ```
 */
expect fun copyToClipboard(text: String): Boolean

/**
 * Reads text from the system clipboard.
 *
 * Attempts to read the current text content from the system clipboard.
 * Returns `null` if the clipboard is empty, contains non-text content,
 * or if reading is not supported on the current platform.
 *
 * @return The clipboard text content, or `null` if unavailable or unsupported.
 * @since 0.1.0
 *
 * @sample
 * ```kotlin
 * val clipboardText = getFromClipboard()
 * clipboardText?.let { text ->
 *     println("Clipboard contains: $text")
 * } ?: println("Clipboard is empty or unavailable")
 * ```
 */
expect fun getFromClipboard(): String?

/**
 * Checks if the clipboard contains text content.
 *
 * This function checks whether the system clipboard currently contains
 * text that can be read. Useful for enabling/disabling paste functionality
 * in UI applications.
 *
 * @return `true` if the clipboard contains text content, `false` otherwise.
 *         Returns `false` on platforms where clipboard reading is not supported.
 * @since 0.1.0
 *
 * @sample
 * ```kotlin
 * if (hasClipboardText()) {
 *     pasteButton.isEnabled = true
 * }
 * ```
 */
expect fun hasClipboardText(): Boolean

/**
 * Clears the clipboard contents.
 *
 * Removes all content from the system clipboard. After calling this function,
 * [hasClipboardText] should return `false` and [getFromClipboard] should
 * return `null`.
 *
 * @since 0.1.0
 *
 * @sample
 * ```kotlin
 * // Clear sensitive data from clipboard
 * clearClipboard()
 * ```
 */
expect fun clearClipboard()
