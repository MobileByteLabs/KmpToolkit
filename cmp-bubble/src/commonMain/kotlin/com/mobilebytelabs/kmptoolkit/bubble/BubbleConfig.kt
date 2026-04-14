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
) {
    companion object {
        val Default = BubbleConfig()
    }
}
