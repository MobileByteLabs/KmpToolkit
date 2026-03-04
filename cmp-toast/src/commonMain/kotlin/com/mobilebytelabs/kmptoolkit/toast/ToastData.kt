package com.mobilebytelabs.kmptoolkit.toast

/**
 * Data class containing all information about a toast to be displayed.
 *
 * @property message The main text content of the toast
 * @property actionLabel Optional label for an action button (e.g., "Undo", "Dismiss")
 * @property duration How long the toast should be displayed
 * @property position Where on the screen to display the toast
 * @property style Visual style of the toast
 * @since 0.1.0
 */
@ConsistentCopyVisibility
data class ToastData internal constructor(
    val message: String,
    val actionLabel: String?,
    val duration: ToastDuration,
    val position: ToastPosition,
    val style: ToastStyle,
    internal val id: Long,
    private val onAction: () -> Unit,
    private val onDismiss: () -> Unit,
) {
    /**
     * Called when the action button is clicked.
     * This will dismiss the toast and trigger [ToastResult.ACTION_PERFORMED].
     */
    fun performAction() {
        onAction()
    }

    /**
     * Called to dismiss the toast.
     * This triggers [ToastResult.DISMISSED].
     */
    fun dismiss() {
        onDismiss()
    }
}

/**
 * Result of showing a toast.
 *
 * @since 0.1.0
 */
enum class ToastResult {
    /**
     * The toast was dismissed (auto-dismiss, swipe, or programmatic).
     */
    DISMISSED,

    /**
     * The action button was clicked.
     */
    ACTION_PERFORMED,
}
