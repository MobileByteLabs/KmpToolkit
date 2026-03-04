package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardObserver
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardObserver

/**
 * Creates and remembers a [ClipboardObserver] that automatically
 * starts observing when entering composition and stops when leaving.
 *
 * This is the recommended way to use clipboard observation in Compose UI.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun ClipboardDemo() {
 *     val observer = rememberClipboardObserver()
 *     val content by observer.clipboardContent.collectAsState()
 *
 *     Column {
 *         Text("Clipboard: ${content ?: "empty"}")
 *         Text("Observing: ${observer.isObserving}")
 *     }
 * }
 * ```
 *
 * ## Lifecycle
 *
 * - Observer starts automatically when the composable enters composition
 * - Observer stops automatically when the composable leaves composition
 * - Safe to use in any composable - resources are properly cleaned up
 *
 * @return A [ClipboardObserver] instance that is lifecycle-aware
 */
@Composable
fun rememberClipboardObserver(): ClipboardObserver {
    val observer = remember { createClipboardObserver() }

    DisposableEffect(observer) {
        observer.startObserving()
        onDispose {
            observer.stopObserving()
        }
    }

    return observer
}
