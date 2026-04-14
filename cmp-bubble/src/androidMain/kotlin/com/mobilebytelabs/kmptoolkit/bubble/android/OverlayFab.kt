package com.mobilebytelabs.kmptoolkit.bubble.android

import android.animation.ValueAnimator
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
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig
import com.mobilebytelabs.kmptoolkit.bubble.BubbleIcon

/**
 * Floating FAB overlay using TYPE_APPLICATION_OVERLAY.
 *
 * Features:
 * - Smooth drag with velocity-based movement
 * - Snap to nearest screen edge after drag
 * - Configurable color, size, text/emoji, elevation
 * - Supports BubbleIcon (text/emoji shown on FAB)
 * - Pulse animation on show
 * - Tap callback
 */
internal class OverlayFab(private val context: Context, private val config: BubbleConfig = BubbleConfig.Default) {

    private var windowManager: WindowManager? = null
    private var fabView: View? = null
    private var isShowing = false
    private var params: WindowManager.LayoutParams? = null

    var onTap: (() -> Unit)? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(title: String = "", icon: BubbleIcon? = null) {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val density = context.resources.displayMetrics.density
        val sizePx = (config.fabSizeDp * density).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels

        // Circular background with configurable color
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(config.fabColor.toInt())
        }

        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = circle
            elevation = config.fabElevationDp * density
        }

        // Determine display text: icon > title initial > config default
        val displayText = when (icon) {
            is BubbleIcon.System -> iconNameToEmoji(icon.name)
            is BubbleIcon.Resource -> config.fabText
            is BubbleIcon.Url -> config.fabText
            null -> if (title.isNotEmpty()) title.first().uppercase() else config.fabText
        }

        val label = TextView(context).apply {
            text = displayText
            setTextColor(config.fabIconColor.toInt())
            textSize = config.fabTextSizeSp
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

        val layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth - sizePx - (16 * density).toInt()
            y = screenHeight - sizePx - (200 * density).toInt()
        }
        params = layoutParams

        // Smooth drag + snap to edge + tap
        setupSmoothDrag(container, layoutParams, screenWidth, sizePx, density)

        try {
            windowManager?.addView(container, layoutParams)
            isShowing = true

            // Entry animation: scale from 0 + overshoot
            container.scaleX = 0f
            container.scaleY = 0f
            container.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()
        } catch (_: Exception) {
            isShowing = false
        }
    }

    fun dismiss() {
        if (!isShowing) return
        val view = fabView ?: return

        // Exit animation: scale down + fade
        view.animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                try {
                    windowManager?.removeView(view)
                } catch (_: Exception) {}
                fabView = null
                isShowing = false
            }
            .start()
    }

    /** Pulse animation — call when content changes (e.g., new URL detected). */
    fun pulse() {
        fabView?.let { view ->
            view.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(150)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .setInterpolator(OvershootInterpolator(2f))
                        .start()
                }
                .start()
        }
    }

    val visible: Boolean get() = isShowing

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSmoothDrag(
        view: View,
        layoutParams: WindowManager.LayoutParams,
        screenWidth: Int,
        sizePx: Int,
        density: Float,
    ) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    // Slight scale up on touch for feedback
                    view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start()
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (dx * dx + dy * dy > 100) isDragging = true
                    if (isDragging) {
                        layoutParams.x = initialX + dx.toInt()
                        layoutParams.y = initialY + dy.toInt()
                        try {
                            windowManager?.updateViewLayout(view, layoutParams)
                        } catch (_: Exception) {}
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    // Scale back to normal
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()

                    if (!isDragging) {
                        onTap?.invoke()
                    } else {
                        // Snap to nearest horizontal edge
                        snapToEdge(layoutParams, screenWidth, sizePx, density)
                    }
                    true
                }

                else -> false
            }
        }
    }

    /** Smoothly animate FAB to the nearest screen edge after drag ends. */
    private fun snapToEdge(layoutParams: WindowManager.LayoutParams, screenWidth: Int, sizePx: Int, density: Float) {
        val margin = (8 * density).toInt()
        val centerX = layoutParams.x + sizePx / 2
        val targetX = if (centerX < screenWidth / 2) {
            margin // snap left
        } else {
            screenWidth - sizePx - margin // snap right
        }

        val animator = ValueAnimator.ofInt(layoutParams.x, targetX)
        animator.duration = 250
        animator.interpolator = OvershootInterpolator(0.8f)
        animator.addUpdateListener { animation ->
            layoutParams.x = animation.animatedValue as Int
            try {
                fabView?.let { windowManager?.updateViewLayout(it, layoutParams) }
            } catch (_: Exception) {}
        }
        animator.start()
    }

    /** Map common icon names to emoji for the FAB. */
    private fun iconNameToEmoji(name: String): String = when (name.lowercase()) {
        "download" -> "\u2B07"
        "share" -> "\uD83D\uDD17"
        "clipboard" -> "\uD83D\uDCCB"
        "person", "chat" -> "\uD83D\uDCAC"
        "play" -> "\u25B6"
        "pause" -> "\u23F8"
        "stop" -> "\u23F9"
        "settings" -> "\u2699"
        "reply" -> "\u21A9"
        "close" -> "\u2715"
        else -> config.fabText
    }
}
