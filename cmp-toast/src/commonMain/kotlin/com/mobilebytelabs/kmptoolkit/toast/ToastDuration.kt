package com.mobilebytelabs.kmptoolkit.toast

/**
 * Duration for how long a toast should be displayed.
 *
 * @property millis The duration in milliseconds. -1 means indefinite.
 * @since 0.1.0
 */
enum class ToastDuration(val millis: Long) {
    /**
     * Short duration (2 seconds).
     * Use for brief confirmations like "Copied" or "Saved".
     */
    SHORT(2000L),

    /**
     * Medium duration (3.5 seconds).
     * Use for messages that need a bit more reading time.
     */
    MEDIUM(3500L),

    /**
     * Long duration (5 seconds).
     * Use for important messages or those with action buttons.
     */
    LONG(5000L),

    /**
     * Indefinite duration - toast remains until explicitly dismissed.
     * Use sparingly, typically with an action button to dismiss.
     */
    INDEFINITE(-1L)
}
