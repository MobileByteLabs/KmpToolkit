package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Icon source for a bubble.
 *
 * @since 0.1.0
 */
sealed class BubbleIcon {
    /** Platform system icon by name (e.g., "download", "share", "clipboard"). */
    data class System(val name: String) : BubbleIcon()

    /** Icon from a URL (loaded asynchronously where supported). */
    data class Url(val url: String) : BubbleIcon()

    /** Icon from app resources by name. */
    data class Resource(val name: String) : BubbleIcon()
}
