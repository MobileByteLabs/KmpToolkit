package com.mobilebytelabs.kmptoolkit.clipboard.worker

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

internal class MingwClipboardWorkerTrigger : ClipboardWorkerTrigger {
    override fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) {
        // Phase 4: Coroutine fallback
    }
}

actual fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger = MingwClipboardWorkerTrigger()
