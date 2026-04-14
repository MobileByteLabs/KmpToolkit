package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Configuration for showing a screen/activity inside a bubble.
 *
 * On Android (API 30+), this configures the Activity displayed inside the Bubble.
 * On other platforms, this influences the notification content or floating window size.
 *
 * @property height Desired height in dp. Use [MATCH_PARENT] for full height.
 * @property width Desired width in dp. Use [MATCH_PARENT] for full width.
 * @property autoExpand Whether to auto-expand the bubble when shown.
 * @since 0.1.0
 */
data class BubbleScreenConfig(
    val height: Int = 400,
    val width: Int = MATCH_PARENT,
    val autoExpand: Boolean = true
) {
    companion object {
        const val MATCH_PARENT = -1
        val Default = BubbleScreenConfig()
    }
}
