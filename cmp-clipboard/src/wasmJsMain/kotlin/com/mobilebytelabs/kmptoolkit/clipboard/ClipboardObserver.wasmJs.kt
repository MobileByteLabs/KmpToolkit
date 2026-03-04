package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WebAssembly JavaScript implementation of ClipboardObserver.
 *
 * Note: Clipboard observation is not fully supported in Wasm JS due to
 * limited browser API access. This is a no-op implementation that returns
 * empty content. Use the regular JS target if clipboard observation is required.
 */
internal class WasmJsClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    override fun startObserving() {
        // No-op: Wasm JS doesn't have full browser API access for clipboard observation
        _isObserving = true
    }

    override fun stopObserving() {
        _isObserving = false
    }
}

actual fun createClipboardObserver(): ClipboardObserver = WasmJsClipboardObserver()
