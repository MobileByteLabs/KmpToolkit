package com.mobilebytelabs.kmptoolkit.clipboard.worker

import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardChange
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardUrlMatcher

/**
 * Triggers a background worker when a URL is detected in the clipboard.
 *
 * This is the bridge between clipboard monitoring and background processing.
 * The consumer app implements the actual worker logic (e.g., downloading content).
 *
 * ## Platform Implementation
 *
 * | Platform | Worker Type |
 * |----------|------------|
 * | Android  | WorkManager OneTimeWorkRequest |
 * | iOS      | BGProcessingTask |
 * | macOS    | DispatchQueue background task |
 * | JVM      | Coroutine on Dispatchers.IO |
 * | JS/Wasm  | Service Worker (if available) |
 * | Others   | Coroutine fallback |
 *
 * ## Usage
 *
 * ```kotlin
 * val trigger = createClipboardWorkerTrigger()
 *
 * // Register a handler
 * trigger.onUrlDetected = { url, matcher, change ->
 *     println("Download: $url from ${matcher.name}")
 *     // Start your download logic here
 * }
 *
 * // Or collect events
 * trigger.workerEvents.collect { event ->
 *     when (event) {
 *         is WorkerEvent.Triggered -> println("Worker started for ${event.url}")
 *         is WorkerEvent.Completed -> println("Worker done")
 *         is WorkerEvent.Failed -> println("Worker failed: ${event.error}")
 *     }
 * }
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardWorkerTrigger {
    /**
     * Called when a monitored URL is detected in the clipboard.
     *
     * @param url The extracted URL.
     * @param matcher The matcher that detected it.
     * @param change The full clipboard change event.
     */
    fun onUrlDetected(url: String, matcher: ClipboardUrlMatcher, change: ClipboardChange)
}

/**
 * Creates a platform-specific [ClipboardWorkerTrigger] instance.
 *
 * @since 0.2.0
 */
expect fun createClipboardWorkerTrigger(): ClipboardWorkerTrigger
