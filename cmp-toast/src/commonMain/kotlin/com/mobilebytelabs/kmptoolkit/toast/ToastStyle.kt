package com.mobilebytelabs.kmptoolkit.toast

/**
 * Visual style for the toast.
 *
 * Each style has a distinct color scheme to convey meaning.
 *
 * @since 0.1.0
 */
enum class ToastStyle {
    /**
     * Default neutral style.
     * Uses the inverse surface color from Material theme.
     */
    DEFAULT,

    /**
     * Success style (green).
     * Use for positive confirmations like "Saved successfully".
     */
    SUCCESS,

    /**
     * Error style (red).
     * Use for error messages like "Failed to save".
     */
    ERROR,

    /**
     * Warning style (orange/amber).
     * Use for warnings like "Connection unstable".
     */
    WARNING,

    /**
     * Info style (blue).
     * Use for informational messages like "New update available".
     */
    INFO,
}
