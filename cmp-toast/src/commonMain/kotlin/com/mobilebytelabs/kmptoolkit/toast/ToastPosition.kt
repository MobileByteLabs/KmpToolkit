package com.mobilebytelabs.kmptoolkit.toast

/**
 * Position where the toast should be displayed on screen.
 *
 * @since 0.1.0
 */
enum class ToastPosition {
    /**
     * Display at the top of the screen.
     * Similar to iOS notification banners.
     */
    TOP,

    /**
     * Display at the center of the screen.
     * Classic Android toast style.
     */
    CENTER,

    /**
     * Display at the bottom of the screen.
     * Material Design snackbar style. This is the default.
     */
    BOTTOM,
}
