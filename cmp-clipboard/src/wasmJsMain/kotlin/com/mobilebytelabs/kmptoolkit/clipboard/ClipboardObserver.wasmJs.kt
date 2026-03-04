package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event

/**
 * WebAssembly JavaScript implementation of ClipboardObserver.
 *
 * Uses document visibilitychange event to detect when the page becomes visible.
 * Note: JavaScript doesn't provide direct clipboard change events, so this
 * implementation only checks the clipboard when the page visibility changes.
 */
internal class WasmJsClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private var visibilityHandler: ((Event) -> Unit)? = null

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        // Listen for visibility changes
        visibilityHandler = { _: Event ->
            if (document.asDynamic().visibilityState == "visible") {
                updateClipboardContent()
            }
        }

        document.addEventListener("visibilitychange", visibilityHandler)

        // Initial read
        updateClipboardContent()
    }

    override fun stopObserving() {
        if (!_isObserving) return
        _isObserving = false

        visibilityHandler?.let { handler ->
            document.removeEventListener("visibilitychange", handler)
        }
        visibilityHandler = null
    }

    private fun updateClipboardContent() {
        // Try to read clipboard using async API
        try {
            val navigator = window.navigator.asDynamic()
            if (navigator.clipboard != undefined) {
                navigator.clipboard.readText().then { text: String? ->
                    _clipboardContent.value = text
                }.catch { _: Throwable ->
                    _clipboardContent.value = null
                }
            }
        } catch (e: Exception) {
            _clipboardContent.value = null
        }
    }
}

actual fun createClipboardObserver(): ClipboardObserver = WasmJsClipboardObserver()
