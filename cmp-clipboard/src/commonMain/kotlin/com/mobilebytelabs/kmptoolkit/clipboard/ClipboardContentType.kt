package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * Types of content that can be stored on the system clipboard.
 *
 * Used by [ClipboardChange] to identify what kind of content was detected.
 *
 * @since 0.2.0
 */
enum class ClipboardContentType {
    /** Plain text content. */
    Text,

    /** A URI/URL (e.g., https://..., content://...). */
    Uri,

    /** HTML-formatted text content. */
    Html,

    /** Image data (bitmap, PNG, etc.). */
    Image,

    /** Content type could not be determined. */
    Unknown,
}

/**
 * Source of the clipboard change — where the content originated.
 *
 * @since 0.2.0
 */
enum class ClipboardSource {
    /** Content was copied from within the current app. */
    Internal,

    /** Content was copied from another app or system component. */
    External,

    /** Source could not be determined. */
    Unknown,
}
