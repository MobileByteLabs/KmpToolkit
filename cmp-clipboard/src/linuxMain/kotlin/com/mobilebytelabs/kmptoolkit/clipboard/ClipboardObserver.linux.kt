package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Linux implementation of ClipboardObserver.
 *
 * Linux doesn't provide a native clipboard change notification mechanism.
 * This implementation provides the basic interface but relies on manual
 * checks via [startObserving] to read the current clipboard state.
 *
 * For active observation, consider implementing a polling mechanism
 * or using a platform-specific library.
 */
internal class LinuxClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        // Initial read
        updateClipboardContent()
    }

    override fun stopObserving() {
        _isObserving = false
    }

    private fun updateClipboardContent() {
        val content = getFromClipboard()
        if (content != _clipboardContent.value) {
            _clipboardContent.value = content
        }
    }
}

actual fun createClipboardObserver(): ClipboardObserver = LinuxClipboardObserver()
