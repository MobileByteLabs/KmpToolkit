package com.mobilebytelabs.kmptoolkit.clipboard.worker

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

internal class MacosClipboardWorkerTrigger : ClipboardWorkerTrigger {
    override fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) {
        // Phase 3: DispatchQueue background task
    }
}

actual fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger = MacosClipboardWorkerTrigger()
