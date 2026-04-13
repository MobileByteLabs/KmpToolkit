package com.mobilebytelabs.kmptoolkit.clipboard.worker

import android.content.Context
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.appContext
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

/**
 * Android implementation of ClipboardWorkerTrigger.
 *
 * Provides a callback-based mechanism for consumer apps to handle URL detections.
 * Consumer apps register their handler which gets called when a matching URL is found.
 *
 * For WorkManager integration, the consumer app implements the actual worker:
 *
 * ```kotlin
 * val trigger = createClipboardWorkerTrigger() as AndroidClipboardWorkerTrigger
 * trigger.urlHandler = { url, matcher, change ->
 *     // Enqueue your WorkManager request here
 *     val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
 *         .setInputData(workDataOf("url" to url, "source" to matcher.name))
 *         .build()
 *     WorkManager.getInstance(context).enqueue(workRequest)
 * }
 * ```
 */
internal class AndroidClipboardWorkerTrigger : ClipboardWorkerTrigger {

    /** Handler called when a URL is detected. Set by the consumer app. */
    var urlHandler: ((url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) -> Unit)? = null

    override fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange) {
        urlHandler?.invoke(url, matcher, change)
    }

    /** Returns the application context for WorkManager initialization. */
    fun getContext(): Context? = appContext
}

actual fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger = AndroidClipboardWorkerTrigger()
