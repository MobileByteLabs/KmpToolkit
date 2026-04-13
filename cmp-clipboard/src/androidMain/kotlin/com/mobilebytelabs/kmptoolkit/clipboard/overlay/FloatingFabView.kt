package com.mobilebytelabs.kmptoolkit.clipboard.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/**
 * Creates the floating action button View for the clipboard overlay.
 *
 * This is a simple circular FAB with a clip icon, created programmatically
 * (no XML resources needed — works as a library).
 */
internal object FloatingFabView {

    private const val FAB_SIZE_DP = 56
    private const val ICON_SIZE_DP = 24
    private const val FAB_COLOR = 0xFF6200EE.toInt() // Material Purple 500
    private const val ICON_COLOR = Color.WHITE

    /**
     * Creates a circular FAB View programmatically.
     */
    fun create(context: Context): View {
        val density = context.resources.displayMetrics.density
        val fabSizePx = (FAB_SIZE_DP * density).toInt()
        val iconSizePx = (ICON_SIZE_DP * density).toInt()

        // Circular background
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(FAB_COLOR)
        }

        // Elevation shadow
        val elevationPx = (6 * density)

        // Container
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(fabSizePx, fabSizePx)
            background = circle
            elevation = elevationPx
        }

        // Icon (using system clip drawable)
        val icon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_edit)
            setColorFilter(ICON_COLOR)
            val padding = ((fabSizePx - iconSizePx) / 2)
            setPadding(padding, padding, padding, padding)
        }
        container.addView(icon, FrameLayout.LayoutParams(fabSizePx, fabSizePx))

        return container
    }
}
