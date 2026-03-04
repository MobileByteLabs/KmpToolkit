package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AppKit.NSApplicationDidBecomeActiveNotification
import platform.AppKit.NSPasteboard
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

/**
 * macOS implementation of ClipboardObserver.
 *
 * Uses [NSPasteboard.changeCount] to track clipboard changes and
 * [NSApplicationDidBecomeActiveNotification] to detect when the app
 * becomes active.
 *
 * macOS doesn't provide a direct clipboard change notification, so this
 * implementation detects changes by comparing the changeCount when
 * the app becomes active.
 */
internal class MacosClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private var lastChangeCount: Long = 0
    private var activeObserver: Any? = null

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        // Track initial change count
        lastChangeCount = NSPasteboard.generalPasteboard.changeCount

        // Observe app becoming active
        activeObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                checkForClipboardChanges()
            },
        )

        // Initial read
        updateClipboardContent()
    }

    override fun stopObserving() {
        if (!_isObserving) return
        _isObserving = false

        activeObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
        activeObserver = null
    }

    private fun checkForClipboardChanges() {
        val currentChangeCount = NSPasteboard.generalPasteboard.changeCount
        if (currentChangeCount != lastChangeCount) {
            lastChangeCount = currentChangeCount
            updateClipboardContent()
        }
    }

    private fun updateClipboardContent() {
        _clipboardContent.value = getFromClipboard()
    }
}

actual fun createClipboardObserver(): ClipboardObserver = MacosClipboardObserver()
