package com.mobilebytelabs.kmptoolkit.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Host composable that displays toasts.
 *
 * Place this at the root of your screen or scaffold to enable toast display.
 *
 * ## Usage
 *
 * ```kotlin
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Your screen content
 *     MyScreenContent()
 *
 *     // Toast host - place at the end to render on top
 *     ToastHost(hostState = toastState)
 * }
 * ```
 *
 * @param hostState The state holder that manages toast display
 * @param modifier Modifier for the host container
 * @param toast Custom composable for rendering the toast. Defaults to [DefaultToast].
 * @since 0.1.0
 */
@Composable
fun ToastHost(
    hostState: ToastHostState,
    modifier: Modifier = Modifier,
    toast: @Composable (ToastData) -> Unit = { DefaultToast(it) }
) {
    val currentToast by hostState.currentToast.collectAsState()

    currentToast?.let { data ->
        // Auto-dismiss after duration
        if (data.duration != ToastDuration.INDEFINITE) {
            LaunchedEffect(data.id) {
                delay(data.duration.millis)
                data.dismiss()
            }
        }

        val alignment = when (data.position) {
            ToastPosition.TOP -> Alignment.TopCenter
            ToastPosition.CENTER -> Alignment.Center
            ToastPosition.BOTTOM -> Alignment.BottomCenter
        }

        val enterTransition = when (data.position) {
            ToastPosition.TOP -> fadeIn() + slideInVertically { -it }
            ToastPosition.CENTER -> fadeIn()
            ToastPosition.BOTTOM -> fadeIn() + slideInVertically { it }
        }

        val exitTransition = when (data.position) {
            ToastPosition.TOP -> fadeOut() + slideOutVertically { -it }
            ToastPosition.CENTER -> fadeOut()
            ToastPosition.BOTTOM -> fadeOut() + slideOutVertically { it }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = if (data.position == ToastPosition.TOP) 48.dp else 0.dp,
                    bottom = if (data.position == ToastPosition.BOTTOM) 48.dp else 0.dp
                ),
            contentAlignment = alignment
        ) {
            AnimatedVisibility(
                visible = true,
                enter = enterTransition,
                exit = exitTransition
            ) {
                toast(data)
            }
        }
    }
}
