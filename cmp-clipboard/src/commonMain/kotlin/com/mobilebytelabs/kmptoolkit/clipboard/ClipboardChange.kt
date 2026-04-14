package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * Represents a clipboard change event with metadata.
 *
 * Emitted by [ClipboardMonitor] whenever the clipboard content changes.
 * Contains the new content, its type, when it changed, and where it came from.
 *
 * ## Usage
 *
 * ```kotlin
 * val monitor = createClipboardMonitor()
 * monitor.start()
 *
 * monitor.changes.collect { change ->
 *     println("Content: ${change.content}")
 *     println("Type: ${change.contentType}")
 *     println("From: ${change.source}")
 *     if (change.isUrl()) {
 *         println("URL detected!")
 *     }
 * }
 * ```
 *
 * @property content The clipboard text content.
 * @property contentType The detected type of the content.
 * @property timestamp Epoch milliseconds when the change was detected.
 * @property source Where the clipboard content originated.
 * @property previousContent The content before this change, or null if unknown.
 * @since 0.2.0
 */
data class ClipboardChange(
    val content: String,
    val contentType: ClipboardContentType = ClipboardContentType.Text,
    val timestamp: Long,
    val source: ClipboardSource = ClipboardSource.Unknown,
    val previousContent: String? = null,
) {
    /**
     * Whether the content looks like a URL.
     */
    fun isUrl(): Boolean = contentType == ClipboardContentType.Uri ||
        content.startsWith("http://") ||
        content.startsWith("https://")

    /**
     * Whether the content changed from the previous value.
     */
    fun hasChanged(): Boolean = previousContent == null || content != previousContent
}
