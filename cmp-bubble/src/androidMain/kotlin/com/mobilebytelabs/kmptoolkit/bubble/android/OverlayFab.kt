package com.mobilebytelabs.kmptoolkit.bubble.android

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Floating FAB overlay using TYPE_APPLICATION_OVERLAY (API 26+) or TYPE_PHONE (<26).
 * Used as fallback when Bubbles API is not available.
 */
internal class OverlayFab(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var fabView: View? = null
    private var isShowing = false

    var onTap: (() -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(title: String = "") {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = context.resources.displayMetrics.density
        val sizePx = (56 * density).toInt()

        // Create circular FAB
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xFF6200EE.toInt())
        }

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = circle
            elevation = 8 * density
        }

        val label = TextView(context).apply {
            text = if (title.isNotEmpty()) title.first().uppercase() else "B"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        container.addView(
            label,
            FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = Gravity.CENTER
            },
        )

        fabView = container

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = (16 * density).toInt()
            y = (100 * density).toInt()
        }

        // Drag + click handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 100) isDragging = true
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY - dy.toInt()
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (_: Exception) {}
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!isDragging) onTap?.invoke()
                    true
                }

                else -> false
            }
        }

        try {
            windowManager?.addView(container, params)
            isShowing = true
        } catch (_: Exception) {
            isShowing = false
        }
    }

    fun dismiss() {
        if (!isShowing) return
        try {
            fabView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        fabView = null
        isShowing = false
    }

    val visible: Boolean get() = isShowing
}
