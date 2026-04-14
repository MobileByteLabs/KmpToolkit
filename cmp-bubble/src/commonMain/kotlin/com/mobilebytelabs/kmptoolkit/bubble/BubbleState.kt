package com.mobilebytelabs.kmptoolkit.bubble

/**
 * Current state of a [Bubble].
 *
 * @since 0.1.0
 */
sealed class BubbleState {
    /** Bubble is not visible. */
    data object Hidden : BubbleState()

    /** Bubble is currently visible on screen. */
    data object Showing : BubbleState()

    /** Bubble was dismissed. */
    data class Dismissed(
        /** True if the user manually dismissed it, false if auto-dismissed or programmatic. */
        val byUser: Boolean,
    ) : BubbleState()

    /** User tapped an action button on the bubble. */
    data class ActionTaken(
        /** The ID of the action that was tapped. */
        val actionId: String,
    ) : BubbleState()
}
