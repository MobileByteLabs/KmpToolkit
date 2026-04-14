package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardHistoryEntry
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardHistory
import com.mobilebytelabs.kmptoolkit.toast.ClipboardToastHost
import com.mobilebytelabs.kmptoolkit.toast.ToastDuration
import com.mobilebytelabs.kmptoolkit.toast.ToastStyle

/**
 * Demo screen showing clipboard observation + history.
 *
 * Features:
 * - Copyable sample texts (tap to copy)
 * - Observer auto-detects clipboard changes
 * - Clipboard history with configurable max size
 * - Copy from history back to clipboard
 * - Clear history
 */
@Composable
fun ClipboardObserverScreen() {
    val observer = rememberClipboardObserver()
    val clipboardContent by observer.clipboardContent.collectAsState()
    var detectedChanges by remember { mutableStateOf(0) }
    var lastContent by remember { mutableStateOf<String?>(null) }

    // Clipboard History (keeps last 15 entries)
    val history = remember { createClipboardHistory(maxSize = 15) }
    val historyEntries by history.entries.collectAsState()

    DisposableEffect(history) {
        history.startCapturing()
        onDispose { history.stopCapturing() }
    }

    // Track changes
    LaunchedEffect(clipboardContent) {
        if (clipboardContent != lastContent && clipboardContent != null) {
            detectedChanges++
            lastContent = clipboardContent
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Clipboard Observer Demo",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap any sample text to copy, then see it detected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Copyable Sample Texts ─────────────────────────────

            Text(
                text = "Tap to Copy",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            val sampleTexts = listOf(
                "Hello, KMP Clipboard!",
                "https://www.instagram.com/reel/ABC123/",
                "https://youtu.be/dQw4w9WgXcQ",
                "The quick brown fox jumps over the lazy dog",
                "https://vm.tiktok.com/ZMF2abc/",
                "KotlinConf 2026 is amazing!",
            )

            sampleTexts.forEach { text ->
                CopyableTextItem(
                    text = text,
                    onCopy = { copyToClipboard(text) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Observer Status ────────────────────────────────────

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (observer.isObserving) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Status", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = if (observer.isObserving) "Active" else "Stopped",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                // Changes Counter
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Changes", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$detectedChanges",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Current Clipboard ─────────────────────────────────

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Current Clipboard", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = clipboardContent ?: "(empty)",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (clipboardContent != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Clipboard History ─────────────────────────────────

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Clipboard History (${historyEntries.size}/${history.maxSize})",
                    style = MaterialTheme.typography.titleSmall,
                )

                if (historyEntries.isNotEmpty()) {
                    OutlinedButton(onClick = { history.clear() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (historyEntries.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        text = "Copy something to start building history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                historyEntries.forEachIndexed { index, entry ->
                    HistoryEntryItem(
                        index = index,
                        entry = entry,
                        onCopy = { history.copyToClipboard(entry) },
                        onRemove = { history.remove(entry) },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Toast host
        ClipboardToastHost(
            clipboardContent = observer.clipboardContent,
            modifier = Modifier.align(Alignment.BottomCenter),
            messageFormatter = { "Copied: ${it.take(40)}" },
            duration = ToastDuration.SHORT,
            style = ToastStyle.SUCCESS,
            maxLength = 50,
            showOnlyOnChange = true,
        )
    }
}

@Composable
private fun CopyableTextItem(
    text: String,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCopy),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (text.startsWith("http")) "\uD83D\uDD17" else "\uD83D\uDCDD",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "TAP TO COPY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun HistoryEntryItem(
    index: Int,
    entry: ClipboardHistoryEntry,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (index == 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.contentType.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "COPY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onCopy).padding(4.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "\u2715",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onRemove).padding(4.dp),
            )
        }
    }
}
