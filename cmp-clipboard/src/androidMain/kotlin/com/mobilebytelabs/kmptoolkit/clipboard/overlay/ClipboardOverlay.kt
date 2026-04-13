package com.mobilebytelabs.kmptoolkit.clipboard.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.mobilebytelabs.kmptoolkit.clipboard.UrlDetection
import com.mobilebytelabs.kmptoolkit.clipboard.appContext

/**
 * Manages a floating overlay window (FAB) for clipboard URL detection.
 *
 * Shows a draggable floating action button on top of other apps using
 * TYPE_APPLICATION_OVERLAY. The FAB animates when a URL is detected
 * and can be tapped to open the app.
 *
 * ## Usage
 *
 * ```kotlin
 * val overlay = ClipboardOverlay()
 * overlay.show()  // Shows the floating FAB
 *
 * // When URL detected
 * overlay.onUrlDetected(detection)  // Animates the FAB
 *
 * overlay.hide()  // Removes the FAB
 * ```
 */
internal class ClipboardOverlay {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var isShowing = false

    /** Callback when the FAB is tapped. */
    var onFabClicked: ((UrlDetection?) -> Unit)? = null

    private var latestDetection: UrlDetection? = null

    /**
     * Shows the floating FAB overlay.
     *
     * Requires SYSTEM_ALERT_WINDOW permission. Check with
     * [OverlayPermission.canDrawOverlays] first.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show(config: ClipboardOverlayConfig = ClipboardOverlayConfig.Default) {
        val context = appContext ?: return
        if (isShowing) return
        if (!OverlayPermission.canDrawOverlays(context)) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create the FAB view
        floatingView = FloatingFabView.create(context)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val gravity = when (config.position) {
            OverlayPosition.TopStart -> Gravity.TOP or Gravity.START
            OverlayPosition.TopEnd -> Gravity.TOP or Gravity.END
            OverlayPosition.BottomStart -> Gravity.BOTTOM or Gravity.START
            OverlayPosition.BottomEnd -> Gravity.BOTTOM or Gravity.END
            OverlayPosition.CenterEnd -> Gravity.CENTER_VERTICAL or Gravity.END
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            x = 0
            y = 100
        }

        // Make draggable
        if (config.draggable) {
            setupDragBehavior(floatingView!!, params)
        }

        // Click handler
        floatingView?.setOnClickListener {
            onFabClicked?.invoke(latestDetection)
        }

        try {
            windowManager?.addView(floatingView, params)
            isShowing = true
        } catch (e: Exception) {
            // Permission might have been revoked
            isShowing = false
        }
    }

    /**
     * Hides and removes the floating overlay.
     */
    fun hide() {
        if (!isShowing) return
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            // View might already be removed
        }
        floatingView = null
        isShowing = false
        latestDetection = null
    }

    /**
     * Called when a URL is detected. Animates the FAB to draw attention.
     */
    fun onUrlDetected(detection: UrlDetection) {
        latestDetection = detection
        floatingView?.let { view ->
            // Pulse animation
            view.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(200)
                .withEndAction {
                    view.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start()
                }
                .start()
        }
    }

    val isVisible: Boolean get() = isShowing

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        view.setOnTouchListener { _, event ->
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
                    if (dx * dx + dy * dy > 100) { // 10px threshold
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        try {
                            windowManager?.updateViewLayout(view, params)
                        } catch (e: Exception) {
                            // View might be removed
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        view.performClick()
                    }
                    true
                }
                else -> false
            }
        }
    }
}
