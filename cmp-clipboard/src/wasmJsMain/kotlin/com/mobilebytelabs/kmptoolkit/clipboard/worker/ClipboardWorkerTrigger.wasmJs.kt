package com.mobilebytelabs.kmptoolkit.clipboard.worker

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

internal class WasmJsClipboardWorkerTrigger : ClipboardWorkerTrigger {
    override fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) {
        // No-op: wasmJs does not support background workers
    }
}

actual fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger = WasmJsClipboardWorkerTrigger()
