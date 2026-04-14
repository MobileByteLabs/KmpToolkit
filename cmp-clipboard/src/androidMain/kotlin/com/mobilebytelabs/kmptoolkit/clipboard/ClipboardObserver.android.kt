package com.mobilebytelabs.kmptoolkit.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of ClipboardObserver.
 *
 * Uses [ClipboardManager.OnPrimaryClipChangedListener] to detect clipboard changes
 * and [ProcessLifecycleOwner] to detect when the app returns to foreground.
 *
 * This implementation automatically checks the clipboard when:
 * 1. The clipboard content changes while the app is in foreground
 * 2. The app returns to foreground from background (with retry for Android 10+ timing)
 *
 * ## Android 10+ (API 29+) Notes
 *
 * Starting with Android 10, apps can only access clipboard content when they have
 * window focus. The `onResume` lifecycle event fires before the window gains focus,
 * so this implementation retries clipboard reads with short delays to ensure
 * content is captured after the window is fully focused.
 */
internal class AndroidClipboardObserver :
    ClipboardObserver,
    DefaultLifecycleObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null
    private val handler = Handler(Looper.getMainLooper())

    private val clipboardManager: ClipboardManager?
        get() = appContext?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        // Register clipboard listener for in-app changes
        clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
            updateClipboardContent()
        }
        clipboardManager?.addPrimaryClipChangedListener(clipboardListener)

        // Register lifecycle observer for foreground detection
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Initial read
        updateClipboardContent()
    }

    override fun stopObserving() {
        if (!_isObserving) return
        _isObserving = false

        clipboardListener?.let { listener ->
            clipboardManager?.removePrimaryClipChangedListener(listener)
        }
        clipboardListener = null

        // Remove all pending retry callbacks
        handler.removeCallbacksAndMessages(null)

        try {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        } catch (e: Exception) {
            // Ignore if lifecycle owner is not available
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        // onStart fires before onResume — try early read
        if (_isObserving) {
            updateClipboardContent()
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // App came to foreground — read immediately + retry with delays
        // On Android 10+, clipboard may not be readable until window has focus,
        // which happens slightly after onResume. Retry at 100ms, 300ms, 600ms.
        if (_isObserving) {
            updateClipboardContent()
            handler.postDelayed({ if (_isObserving) updateClipboardContent() }, 100)
            handler.postDelayed({ if (_isObserving) updateClipboardContent() }, 300)
            handler.postDelayed({ if (_isObserving) updateClipboardContent() }, 600)
        }
    }

    private fun updateClipboardContent() {
        val content = getFromClipboard()
        if (content != _clipboardContent.value) {
            _clipboardContent.value = content
        }
    }
}

actual fun createClipboardObserver(): ClipboardObserver = AndroidClipboardObserver()
