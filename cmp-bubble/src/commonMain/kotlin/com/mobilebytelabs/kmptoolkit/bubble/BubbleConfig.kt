package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Global configuration for the [Bubble] system.
 *
 * @property defaultStyle Default style when none is specified in [Bubble.show].
 * @property channelId Android notification channel ID.
 * @property channelName Android notification channel display name.
 * @property vibrate Whether to vibrate on bubble show (mobile only).
 * @property sound Whether to play a sound on bubble show.
 * @since 0.1.0
 */
data class BubbleConfig(
    val defaultStyle: BubbleStyle = BubbleStyle.Auto,
    val channelId: String = "kmptoolkit_bubble",
    val channelName: String = "Bubbles",
    val vibrate: Boolean = false,
    val sound: Boolean = false,
    /** FAB background color as ARGB long (e.g., 0xFF6200EE for purple). */
    val fabColor: Long = 0xFF6200EE,
    /** FAB icon/text color as ARGB long. */
    val fabIconColor: Long = 0xFFFFFFFF,
    /** FAB size in dp. */
    val fabSizeDp: Int = 56,
    /** FAB text shown when no icon is provided (1-2 chars, e.g., emoji). */
    val fabText: String = "\u2B07",
    /** FAB text size in sp. */
    val fabTextSizeSp: Float = 20f,
    /** FAB elevation in dp. */
    val fabElevationDp: Int = 8,
) {
    companion object {
        val Default = BubbleConfig()

        /** Download-style FAB (like InSaver). */
        val Download = BubbleConfig(
            fabColor = 0xFFE91E63,
            fabText = "\u2B07",
            fabSizeDp = 56,
        )

        /** Chat-style FAB. */
        val Chat = BubbleConfig(
            fabColor = 0xFF2196F3,
            fabText = "\uD83D\uDCAC",
            fabSizeDp = 56,
        )
    }
}
