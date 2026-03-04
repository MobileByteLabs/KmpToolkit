package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.FlavorEvent
import java.awt.datatransfer.FlavorListener

/**
 * JVM Desktop implementation of ClipboardObserver.
 *
 * Uses [FlavorListener] to detect clipboard changes on the AWT system clipboard.
 *
 * Note: FlavorListener may be unreliable on some systems. The listener is called
 * when the data flavors change, which typically happens when new content is
 * copied to the clipboard.
 */
internal class JvmClipboardObserver : ClipboardObserver, FlavorListener {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private val clipboard by lazy {
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (e: Exception) {
            null
        }
    }

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        try {
            clipboard?.addFlavorListener(this)
        } catch (e: Exception) {
            // Headless environment or clipboard not available
        }
        updateClipboardContent()
    }

    override fun stopObserving() {
        if (!_isObserving) return
        _isObserving = false

        try {
            clipboard?.removeFlavorListener(this)
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    override fun flavorsChanged(e: FlavorEvent?) {
        updateClipboardContent()
    }

    private fun updateClipboardContent() {
        _clipboardContent.value = try {
            val cb = clipboard ?: return
            if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                cb.getData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

actual fun createClipboardObserver(): ClipboardObserver = JvmClipboardObserver()
