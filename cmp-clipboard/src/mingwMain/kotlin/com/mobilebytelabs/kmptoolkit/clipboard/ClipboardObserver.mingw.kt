package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Windows (MinGW) implementation of ClipboardObserver.
 *
 * Windows provides clipboard viewer chain for notifications, but implementing
 * it in native Kotlin requires Win32 API calls that are complex.
 *
 * This implementation provides the basic interface and reads the current
 * clipboard state on [startObserving]. For active observation, consider
 * implementing a polling mechanism or using Win32 clipboard viewer chain.
 */
internal class MingwClipboardObserver : ClipboardObserver {
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

actual fun createClipboardObserver(): ClipboardObserver = MingwClipboardObserver()
