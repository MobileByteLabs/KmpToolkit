package com.mobilebytelabs.kmptoolkit.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A convenience composable that automatically shows a toast when clipboard content changes.
 *
 * This integrates with the `cmp-clipboard` module's ClipboardObserver to provide
 * automatic clipboard notifications.
 *
 * ## Usage
 *
 * ```kotlin
 * val observer = rememberClipboardObserver()
 *
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // Your content
 *
 *     ClipboardToastHost(
 *         clipboardContent = observer.clipboardContent,
 *         messageFormatter = { "Copied: $it" },
 *         duration = ToastDuration.SHORT
 *     )
 * }
 * ```
 *
 * @param clipboardContent StateFlow of clipboard content from ClipboardObserver
 * @param modifier Modifier for the toast host
 * @param hostState Optional custom ToastHostState
 * @param messageFormatter Function to format the clipboard content for display
 * @param duration How long to show the toast
 * @param position Where to show the toast
 * @param style Visual style of the toast
 * @param maxLength Maximum characters to display (content is truncated with "...")
 * @param showOnlyOnChange If true, only shows toast when content actually changes
 * @since 0.1.0
 */
@Composable
fun ClipboardToastHost(
    clipboardContent: StateFlow<String?>,
    modifier: Modifier = Modifier,
    hostState: ToastHostState = rememberToastHostState(),
    messageFormatter: (String) -> String = { "Copied: $it" },
    duration: ToastDuration = ToastDuration.SHORT,
    position: ToastPosition = ToastPosition.BOTTOM,
    style: ToastStyle = ToastStyle.DEFAULT,
    maxLength: Int = 50,
    showOnlyOnChange: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var lastNotifiedContent by remember { mutableStateOf<String?>(null) }
    var isFirstEmission by remember { mutableStateOf(true) }

    LaunchedEffect(clipboardContent) {
        clipboardContent.collect { content ->
            // Skip first emission if showOnlyOnChange is true
            if (isFirstEmission) {
                isFirstEmission = false
                if (showOnlyOnChange) {
                    lastNotifiedContent = content
                    return@collect
                }
            }

            if (content != null && content != lastNotifiedContent) {
                lastNotifiedContent = content

                val displayText = if (content.length > maxLength) {
                    content.take(maxLength) + "..."
                } else {
                    content
                }

                scope.launch {
                    hostState.showToast(
                        message = messageFormatter(displayText),
                        duration = duration,
                        position = position,
                        style = style
                    )
                }
            }
        }
    }

    ToastHost(
        hostState = hostState,
        modifier = modifier
    )
}

/**
 * Extension function to easily show a toast for copied content.
 *
 * @param content The content that was copied
 * @param duration Toast duration
 * @param maxLength Maximum characters to display
 * @since 0.1.0
 */
suspend fun ToastHostState.showCopiedToast(
    content: String,
    duration: ToastDuration = ToastDuration.SHORT,
    maxLength: Int = 50
): ToastResult {
    val displayText = if (content.length > maxLength) {
        content.take(maxLength) + "..."
    } else {
        content
    }

    return showToast(
        message = "Copied: $displayText",
        duration = duration,
        style = ToastStyle.DEFAULT
    )
}
