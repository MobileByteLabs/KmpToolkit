package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.WatchKit.WKApplicationDidBecomeActiveNotification
import platform.UIKit.UIPasteboard

/**
 * watchOS implementation of ClipboardObserver.
 *
 * Uses [UIPasteboard.changeCount] to track clipboard changes and
 * [WKApplicationDidBecomeActiveNotification] to detect when the app
 * returns to foreground.
 *
 * Note: watchOS has limited clipboard functionality.
 */
internal class WatchosClipboardObserver : ClipboardObserver {
    private val _clipboardContent = MutableStateFlow<String?>(null)
    override val clipboardContent: StateFlow<String?> = _clipboardContent.asStateFlow()

    private var _isObserving = false
    override val isObserving: Boolean get() = _isObserving

    private var lastChangeCount: Long = 0
    private var foregroundObserver: Any? = null

    override fun startObserving() {
        if (_isObserving) return
        _isObserving = true

        // Track initial change count
        lastChangeCount = UIPasteboard.generalPasteboard.changeCount

        // Observe app becoming active (foreground)
        foregroundObserver = NSNotificationCenter.defaultCenter.addObserverForName(
            name = WKApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
            usingBlock = { _ ->
                checkForClipboardChanges()
            }
        )

        // Initial read
        updateClipboardContent()
    }

    override fun stopObserving() {
        if (!_isObserving) return
        _isObserving = false

        foregroundObserver?.let { observer ->
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
        foregroundObserver = null
    }

    private fun checkForClipboardChanges() {
        val currentChangeCount = UIPasteboard.generalPasteboard.changeCount
        if (currentChangeCount != lastChangeCount) {
            lastChangeCount = currentChangeCount
            updateClipboardContent()
        }
    }

    private fun updateClipboardContent() {
        _clipboardContent.value = UIPasteboard.generalPasteboard.string
    }
}

actual fun createClipboardObserver(): ClipboardObserver = WatchosClipboardObserver()
