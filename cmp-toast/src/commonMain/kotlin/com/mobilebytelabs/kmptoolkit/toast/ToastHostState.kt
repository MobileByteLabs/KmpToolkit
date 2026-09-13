package com.mobilebytelabs.kmptoolkit.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * State holder for managing toast display.
 *
 * Use [rememberToastHostState] to create an instance in a composable.
 *
 * ## Usage
 *
 * ```kotlin
 * val toastState = rememberToastHostState()
 * val scope = rememberCoroutineScope()
 *
 * Button(onClick = {
 *     scope.launch {
 *         toastState.showToast("Hello!")
 *     }
 * }) {
 *     Text("Show Toast")
 * }
 *
 * ToastHost(hostState = toastState)
 * ```
 *
 * @since 0.1.0 *
 * Implements [ToastDispatcher], so a ViewModel can depend on that interface rather than on this
 * class — the API here was already Compose-free, it simply had no type to substitute.
 */
class ToastHostState : ToastDispatcher {
    private val mutex = Mutex()
    private var currentContinuation: CancellableContinuation<ToastResult>? = null
    private var idCounter = 0L

    private val _currentToast = MutableStateFlow<ToastData?>(null)

    /**
     * The currently displayed toast, or null if none.
     */
    val currentToast: StateFlow<ToastData?> = _currentToast.asStateFlow()

    /**
     * Shows a toast with the specified parameters.
     *
     * This function suspends until the toast is dismissed (either automatically
     * after [duration] or by user interaction).
     *
     * If another toast is currently being displayed, this call will wait
     * until the previous toast is dismissed before showing the new one.
     *
     * @param message The text to display
     * @param actionLabel Optional label for an action button
     * @param duration How long to display the toast
     * @param position Where to display the toast on screen
     * @param style Visual style of the toast
     * @return The result indicating how the toast was dismissed
     */
    // No default values here: an override may not restate them. They live on ToastDispatcher,
    // and call sites — including `toastHostState.showToast("hi")` on the concrete type — still
    // inherit them from the interface declaration.
    override suspend fun showToast(
        message: String,
        actionLabel: String?,
        duration: ToastDuration,
        position: ToastPosition,
        style: ToastStyle,
    ): ToastResult = mutex.withLock {
        // The message is NOT reported: a toast body is user-facing copy that routinely embeds
        // names, amounts and error detail. Shape only — duration, position, style, has-action.
        observeLifecycle(
            cmpMetadata(),
            "toast_shown",
            mapOf(
                "duration" to duration.name,
                "position" to position.name,
                "style" to style::class.simpleName,
                "hasAction" to (actionLabel != null),
            ),
        )
        try {
            suspendCancellableCoroutine { continuation ->
                currentContinuation = continuation
                val id = ++idCounter

                val toast = ToastData(
                    message = message,
                    actionLabel = actionLabel,
                    duration = duration,
                    position = position,
                    style = style,
                    id = id,
                    onAction = {
                        if (_currentToast.value?.id == id) {
                            _currentToast.value = null
                            currentContinuation?.resume(ToastResult.ACTION_PERFORMED)
                            currentContinuation = null
                        }
                    },
                    onDismiss = {
                        if (_currentToast.value?.id == id) {
                            _currentToast.value = null
                            currentContinuation?.resume(ToastResult.DISMISSED)
                            currentContinuation = null
                        }
                    },
                )

                _currentToast.value = toast

                continuation.invokeOnCancellation {
                    if (_currentToast.value?.id == id) {
                        _currentToast.value = null
                    }
                }
            }
        } finally {
            currentContinuation = null
        }
    }

    /**
     * Dismisses the currently displayed toast, if any.
     */
    fun dismiss() {
        _currentToast.value?.dismiss()
    }
}

/**
 * Creates and remembers a [ToastHostState].
 *
 * @return A remembered [ToastHostState] instance
 * @since 0.1.0
 */
@Composable
fun rememberToastHostState(): ToastHostState = remember { ToastHostState() }
