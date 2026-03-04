package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WASI implementation of ClipboardObserver.
 *
 * WASI (WebAssembly System Interface) does not have access to system clipboard,
 * so this is a no-op stub implementation that always returns null.
 */
internal class WasmWasiClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    override fun startObserving() {
        _isObserving = true
        // WASI has no clipboard access - content stays null
    }

    override fun stopObserving() {
        _isObserving = false
    }
}

actual fun createClipboardObserver(): ClipboardObserver = WasmWasiClipboardObserver()
