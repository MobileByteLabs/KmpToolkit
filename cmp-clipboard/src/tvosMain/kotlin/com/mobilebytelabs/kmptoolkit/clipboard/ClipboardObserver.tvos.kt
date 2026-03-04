package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * tvOS implementation of ClipboardObserver.
 *
 * Note: tvOS does not have pasteboard/clipboard API access.
 * This is a no-op stub implementation.
 */
internal class TvosClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    override fun startObserving() {
        // No-op: tvOS has no clipboard access
        _isObserving = true
    }

    override fun stopObserving() {
        _isObserving = false
    }
}

actual fun createClipboardObserver(): ClipboardObserver = TvosClipboardObserver()
