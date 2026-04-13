package com.mobilebytesensei.remoteconfig.dynamic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mobilebytesensei.remoteconfig.dynamic.model.UiAction
import com.mobilebytesensei.remoteconfig.dynamic.model.UiButtonStyle
import com.mobilebytesensei.remoteconfig.dynamic.model.UiNode
import com.mobilebytesensei.remoteconfig.dynamic.model.UiTextStyle

@Composable
fun DynamicUiRenderer(node: UiNode, onAction: (UiAction) -> Unit) {
    when (node) {
        is UiNode.Column -> DynamicColumn(node, onAction)
        is UiNode.Row -> DynamicRow(node, onAction)
        is UiNode.Box -> DynamicBox(node, onAction)
        is UiNode.Text -> DynamicText(node)
        is UiNode.Image -> DynamicImage(node)
        is UiNode.Button -> DynamicButton(node, onAction)
        is UiNode.Spacer -> DynamicSpacer(node)
        is UiNode.Divider -> DynamicDivider(node)
        is UiNode.Card -> DynamicCard(node, onAction)
        is UiNode.Badge -> DynamicBadge(node)
        is UiNode.Icon -> DynamicIcon(node)
    }
}

@Composable
private fun DynamicColumn(node: UiNode.Column, onAction: (UiAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(node.padding.dp)
            .maybeBackground(node.background),
        verticalArrangement = Arrangement.spacedBy(node.spacing.dp),
        horizontalAlignment = node.alignment.toHorizontalAlignment(),
    ) {
        node.children.forEach { DynamicUiRenderer(it, onAction) }
    }
}

@Composable
private fun DynamicRow(node: UiNode.Row, onAction: (UiAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(node.padding.dp),
        horizontalArrangement = Arrangement.spacedBy(node.spacing.dp),
        verticalAlignment = node.alignment.toVerticalAlignment(),
    ) {
        node.children.forEach { child ->
            if (child is UiNode.Column || child is UiNode.Text) {
                Box(modifier = Modifier.weight(1f)) {
                    DynamicUiRenderer(child, onAction)
                }
            } else {
                DynamicUiRenderer(child, onAction)
            }
        }
    }
}

@Composable
private fun DynamicBox(node: UiNode.Box, onAction: (UiAction) -> Unit) {
    Box(
        modifier = Modifier
            .then(if (node.width != null) Modifier.width(node.width.dp) else Modifier.fillMaxWidth())
            .then(if (node.height != null) Modifier.height(node.height.dp) else Modifier)
            .padding(node.padding.dp)
            .maybeBackground(node.background),
        contentAlignment = when (node.alignment) {
            "center" -> Alignment.Center
            "topEnd" -> Alignment.TopEnd
            "bottomCenter" -> Alignment.BottomCenter
            else -> Alignment.TopStart
        },
    ) {
        node.children.forEach { DynamicUiRenderer(it, onAction) }
    }
}

@Composable
private fun DynamicText(node: UiNode.Text) {
    Text(
        text = node.content,
        style = when (node.style) {
            UiTextStyle.HEADLINE -> MaterialTheme.typography.headlineSmall
            UiTextStyle.TITLE -> MaterialTheme.typography.titleMedium
            UiTextStyle.BODY -> MaterialTheme.typography.bodyMedium
            UiTextStyle.CAPTION -> MaterialTheme.typography.bodySmall
            UiTextStyle.LABEL -> MaterialTheme.typography.labelMedium
        },
        color = node.color?.parseColor() ?: Color.Unspecified,
        fontWeight = when (node.fontWeight) {
            "bold" -> FontWeight.Bold
            "semibold" -> FontWeight.SemiBold
            "medium" -> FontWeight.Medium
            "light" -> FontWeight.Light
            else -> null
        },
        textAlign = when (node.align) {
            "center" -> TextAlign.Center
            "end" -> TextAlign.End
            else -> TextAlign.Start
        },
        maxLines = node.maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DynamicImage(node: UiNode.Image) {
    AsyncImage(
        model = node.url,
        contentDescription = null,
        contentScale = when (node.fit) {
            "fit" -> ContentScale.Fit
            "fill" -> ContentScale.FillBounds
            "inside" -> ContentScale.Inside
            else -> ContentScale.Crop
        },
        modifier = Modifier
            .then(if (node.width != null) Modifier.width(node.width.dp) else Modifier.fillMaxWidth())
            .then(if (node.height != null) Modifier.height(node.height.dp) else Modifier)
            .clip(RoundedCornerShape(node.cornerRadius.dp)),
    )
}

@Composable
private fun DynamicButton(node: UiNode.Button, onAction: (UiAction) -> Unit) {
    val onClick = { node.action?.let { onAction(it) } ?: Unit }

    when (node.style) {
        UiButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = node.color?.parseColor()?.let {
                ButtonDefaults.buttonColors(containerColor = it)
            } ?: ButtonDefaults.buttonColors(),
        ) {
            Text(node.text, fontWeight = FontWeight.SemiBold)
        }

        UiButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(node.text)
        }

        UiButtonStyle.TEXT -> TextButton(onClick = onClick) {
            Text(node.text, color = node.color?.parseColor() ?: Color.Unspecified)
        }
    }
}

@Composable
private fun DynamicSpacer(node: UiNode.Spacer) {
    if (node.height > 0) Spacer(modifier = Modifier.height(node.height.dp))
    if (node.width > 0) Spacer(modifier = Modifier.width(node.width.dp))
}

@Composable
private fun DynamicDivider(node: UiNode.Divider) {
    HorizontalDivider(
        thickness = node.thickness.dp,
        color = node.color?.parseColor() ?: MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun DynamicCard(node: UiNode.Card, onAction: (UiAction) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(node.cornerRadius.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = node.elevation.dp),
    ) {
        Column(modifier = Modifier.padding(node.padding.dp)) {
            node.children.forEach { DynamicUiRenderer(it, onAction) }
        }
    }
}

@Composable
private fun DynamicBadge(node: UiNode.Badge) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = node.backgroundColor?.parseColor()
            ?: MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = node.text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = node.color?.parseColor() ?: MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun DynamicIcon(node: UiNode.Icon) {
    Text(text = node.emoji, fontSize = node.size.sp)
}

// Helpers
private fun String.parseColor(): Color? {
    if (!startsWith("#") || length < 7) return null
    return try {
        Color(("FF" + removePrefix("#")).toLong(16))
    } catch (_: Exception) {
        null
    }
}

private fun Modifier.maybeBackground(color: String?): Modifier {
    val parsed = color?.parseColor() ?: return this
    return this.background(parsed)
}

private fun String.toHorizontalAlignment(): Alignment.Horizontal = when (this) {
    "center" -> Alignment.CenterHorizontally
    "end" -> Alignment.End
    else -> Alignment.Start
}

private fun String.toVerticalAlignment(): Alignment.Vertical = when (this) {
    "center" -> Alignment.CenterVertically
    "bottom" -> Alignment.Bottom
    else -> Alignment.Top
}
