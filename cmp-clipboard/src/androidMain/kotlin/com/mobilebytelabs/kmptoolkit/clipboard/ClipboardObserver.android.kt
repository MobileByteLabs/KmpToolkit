package com.mobilebytelabs.kmptoolkit.clipboard

import android.content.ClipboardManager
import android.content.Context
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
 * 2. The app returns to foreground from background
 */
internal class AndroidClipboardObserver : ClipboardObserver, DefaultLifecycleObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

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

        try {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        } catch (e: Exception) {
            // Ignore if lifecycle owner is not available
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        // App came to foreground - check clipboard for changes made in other apps
        updateClipboardContent()
    }

    private fun updateClipboardContent() {
        val content = getFromClipboard()
        if (content != _clipboardContent.value) {
            _clipboardContent.value = content
        }
    }
}

actual fun createClipboardObserver(): ClipboardObserver = AndroidClipboardObserver()
