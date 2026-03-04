package com.mobilebytelabs.kmptoolkit.toast

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Default toast composable with Material Design 3 styling.
 *
 * Features:
 * - Style-based colors (success=green, error=red, etc.)
 * - Optional action button
 * - Swipe to dismiss
 * - Smooth animations
 *
 * @param data The toast data to display
 * @since 0.1.0
 */
@Composable
fun DefaultToast(data: ToastData) {
    var offsetX by remember { mutableStateOf(0f) }
    val dismissThreshold = 150f

    val backgroundColor = when (data.style) {
        ToastStyle.DEFAULT -> MaterialTheme.colorScheme.inverseSurface
        ToastStyle.SUCCESS -> Color(0xFF2E7D32) // Green 800
        ToastStyle.ERROR -> Color(0xFFC62828) // Red 800
        ToastStyle.WARNING -> Color(0xFFEF6C00) // Orange 800
        ToastStyle.INFO -> Color(0xFF1565C0) // Blue 800
    }

    val contentColor = when (data.style) {
        ToastStyle.DEFAULT -> MaterialTheme.colorScheme.inverseOnSurface
        else -> Color.White
    }

    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 400.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .pointerInput(data.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX.absoluteValue > dismissThreshold) {
                            data.dismiss()
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = {
                        offsetX = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX += dragAmount
                    }
                )
            }
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = if (data.actionLabel != null) 8.dp else 16.dp,
                top = 14.dp,
                bottom = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            data.actionLabel?.let { label ->
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = { data.performAction() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = when (data.style) {
                            ToastStyle.DEFAULT -> MaterialTheme.colorScheme.inversePrimary
                            else -> Color.White.copy(alpha = 0.95f)
                        }
                    )
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
