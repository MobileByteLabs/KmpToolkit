package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.StateFlow

/**
 * Clipboard history manager that keeps the last N clipboard entries.
 *
 * Automatically captures clipboard changes via [ClipboardObserver] and
 * maintains a configurable history stack. Newest entries are at index 0.
 *
 * ## Usage
 *
 * ```kotlin
 * val history = createClipboardHistory(maxSize = 20)
 * history.startCapturing()
 *
 * // Observe history changes
 * history.entries.collect { entries ->
 *     entries.forEach { entry ->
 *         println("[${entry.timestamp}] ${entry.content}")
 *     }
 * }
 *
 * // Copy from history back to clipboard
 * history.copyToClipboard(entry)
 *
 * // Clear all history
 * history.clear()
 *
 * history.stopCapturing()
 * ```
 *
 * ## Compose Usage
 *
 * ```kotlin
 * @Composable
 * fun ClipboardHistoryView() {
 *     val history = remember { createClipboardHistory(maxSize = 20) }
 *     val entries by history.entries.collectAsState()
 *
 *     DisposableEffect(history) {
 *         history.startCapturing()
 *         onDispose { history.stopCapturing() }
 *     }
 *
 *     LazyColumn {
 *         items(entries) { entry ->
 *             Text(entry.content)
 *         }
 *     }
 * }
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardHistory {
    /** All history entries, newest first. */
    val entries: StateFlow<List<ClipboardHistoryEntry>>

    /** Maximum number of entries to keep. */
    val maxSize: Int

    /** Whether the history is currently capturing clipboard changes. */
    val isCapturing: Boolean

    /** Start capturing clipboard changes into history. */
    fun startCapturing()

    /** Stop capturing clipboard changes. */
    fun stopCapturing()

    /** Copy a history entry back to the system clipboard. */
    fun copyToClipboard(entry: ClipboardHistoryEntry): Boolean

    /** Remove a specific entry from history. */
    fun remove(entry: ClipboardHistoryEntry)

    /** Clear all history entries. */
    fun clear()
}

/**
 * A single clipboard history entry.
 *
 * @property content The clipboard text content.
 * @property timestamp Epoch milliseconds when the content was captured.
 * @property contentType The detected content type.
 * @since 0.2.0
 */
data class ClipboardHistoryEntry(
    val content: String,
    val timestamp: Long,
    val contentType: ClipboardContentType = ClipboardContentType.Text
)

/**
 * Creates a [ClipboardHistory] instance with the specified maximum size.
 *
 * @param maxSize Maximum number of entries to keep (oldest entries are evicted). Default: 20.
 * @return A new ClipboardHistory instance.
 * @since 0.2.0
 */
fun createClipboardHistory(maxSize: Int = 20): ClipboardHistory =
    DefaultClipboardHistory(maxSize)
