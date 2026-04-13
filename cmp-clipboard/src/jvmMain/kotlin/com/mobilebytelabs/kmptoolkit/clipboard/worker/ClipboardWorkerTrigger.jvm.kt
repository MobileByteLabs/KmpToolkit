package com.mobilebytelabs.kmptoolkit.clipboard.worker

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

internal class JvmClipboardWorkerTrigger : ClipboardWorkerTrigger {
    override fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) {
        // Phase 4: Coroutine on Dispatchers.IO
    }
}

actual fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger = JvmClipboardWorkerTrigger()
