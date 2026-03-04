package com.mobilebytelabs.kmptoolkit.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * @since 0.1.0
 */
class ToastHostState {
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
    suspend fun showToast(
        message: String,
        actionLabel: String? = null,
        duration: ToastDuration = ToastDuration.SHORT,
        position: ToastPosition = ToastPosition.BOTTOM,
        style: ToastStyle = ToastStyle.DEFAULT,
    ): ToastResult = mutex.withLock {
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
