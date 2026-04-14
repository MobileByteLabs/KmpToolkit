package com.mobilebytelabs.kmptoolkit.bubble

/**
 * What happens when the user taps the bubble body (not an action button).
 *
 * @since 0.1.0
 */
sealed class BubbleTapAction {
    /** No action on tap. */
    data object None : BubbleTapAction()

    /** Dismiss the bubble on tap. */
    data object Dismiss : BubbleTapAction()

    /**
     * Open a deep link / route when tapped.
     *
     * On Android, this opens the Activity associated with the URI.
     * On iOS, this opens the app with the URI in the launch options.
     */
    data class DeepLink(val uri: String) : BubbleTapAction()

    /** Custom callback on tap. */
    data class Callback(val onTap: () -> Unit) : BubbleTapAction()
}
