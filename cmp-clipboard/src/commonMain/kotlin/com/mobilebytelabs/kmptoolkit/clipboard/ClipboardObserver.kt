package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.coroutines.flow.StateFlow

/**
 * Observes clipboard changes and emits updates via StateFlow.
 *
 * The observer automatically detects when the clipboard content changes,
 * particularly useful for detecting content copied in other apps when
 * your app returns to the foreground.
 *
 * ## Usage
 *
 * ```kotlin
 * val observer = createClipboardObserver()
 * observer.startObserving()
 *
 * // Collect changes
 * observer.clipboardContent.collect { content ->
 *     println("Clipboard changed: $content")
 * }
 *
 * // When done
 * observer.stopObserving()
 * ```
 *
 * ## Compose Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val observer = rememberClipboardObserver()
 *     val content by observer.clipboardContent.collectAsState()
 *
 *     Text("Clipboard: $content")
 * }
 * ```
 *
 * ## Platform Support
 *
 * | Platform | Support | Method |
 * |----------|:-------:|--------|
 * | Android  | Full    | ClipboardManager listener + ProcessLifecycleOwner |
 * | iOS      | Full    | UIPasteboard.changeCount + App lifecycle |
 * | JVM      | Full    | FlavorListener |
 * | macOS    | Full    | NSPasteboard.changeCount + polling |
 * | JS/Wasm  | Limited | Visibility change only |
 * | Linux    | Limited | Polling |
 * | Windows  | Limited | Polling |
 *
 * @since 0.1.0
 */
interface ClipboardObserver {
    /**
     * Current clipboard content as a StateFlow.
     *
     * Emits `null` when the clipboard is empty or unavailable.
     * Updates are emitted whenever the clipboard content changes,
     * including when the app returns to foreground.
     */
    val clipboardContent: StateFlow<String?>

    /**
     * Starts observing clipboard changes.
     *
     * On mobile platforms, observation is lifecycle-aware and will
     * automatically check for changes when the app returns to foreground.
     *
     * Safe to call multiple times - subsequent calls are no-ops if
     * already observing.
     */
    fun startObserving()

    /**
     * Stops observing clipboard changes and releases resources.
     *
     * Should be called when observation is no longer needed to prevent
     * resource leaks. Safe to call multiple times.
     */
    fun stopObserving()

    /**
     * Whether the observer is currently active.
     *
     * Returns `true` if [startObserving] was called and [stopObserving]
     * has not been called since.
     */
    val isObserving: Boolean
}

/**
 * Creates a platform-specific ClipboardObserver.
 *
 * Each call creates a new observer instance. For Compose usage,
 * prefer [rememberClipboardObserver] which handles lifecycle automatically.
 *
 * @return A new ClipboardObserver instance
 * @since 0.1.0
 *
 * @sample
 * ```kotlin
 * val observer = createClipboardObserver()
 * observer.startObserving()
 *
 * // In a coroutine
 * observer.clipboardContent.collect { content ->
 *     if (content != null) {
 *         println("Detected clipboard: $content")
 *     }
 * }
 * ```
 */
expect fun createClipboardObserver(): ClipboardObserver
