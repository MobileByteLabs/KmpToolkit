package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mobilebytelabs.kmptoolkit.clipboard.monitor.ClipboardMonitorState

/**
 * Unified ClipboardManager demo using ViewModel (MVI pattern).
 *
 * Shows how a single ClipboardViewModel backed by ClipboardManager provides:
 * - Sync & async copy/paste
 * - Auto-observing content via UiState
 * - Clipboard history with configurable size
 * - URL detection with social media matchers
 * - All state managed in ViewModel, survives config changes
 */
@Composable
fun ClipboardManagerScreen(
    viewModel: ClipboardViewModel = viewModel { ClipboardViewModel() },
) {
    val state by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "ClipboardManager + ViewModel",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = "MVI pattern — state survives rotation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Tap to Copy Samples ─────────────────────────────────
        Text("Tap to Copy", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))

        val samples = listOf(
            "Hello, ClipboardManager!",
            "https://www.instagram.com/reel/ABC123/",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://vm.tiktok.com/ZMF2abc/",
            "Kotlin Multiplatform is awesome",
            "https://x.com/user/status/12345",
        )

        samples.forEach { text ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.onAction(ClipboardAction.Copy(text)) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (text.startsWith("http")) "\uD83D\uDD17" else "\uD83D\uDCDD")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text, style = MaterialTheme.typography.bodySmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text("TAP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Custom Input ────────────────────────────────────────
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Custom text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.onAction(ClipboardAction.Copy(inputText)) },
                modifier = Modifier.weight(1f),
                enabled = inputText.isNotEmpty(),
            ) { Text("Copy") }

            Button(
                onClick = { viewModel.onAction(ClipboardAction.CopyAsync(inputText)) },
                modifier = Modifier.weight(1f),
                enabled = inputText.isNotEmpty(),
            ) { Text("Async") }

            Button(
                onClick = { viewModel.onAction(ClipboardAction.PasteAsync) },
                modifier = Modifier.weight(1f),
            ) { Text("Read") }
        }

        // ── Status ──────────────────────────────────────────────
        if (state.statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(state.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Live Content + Monitor ──────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusCard(
                title = "Live Content",
                value = state.currentContent?.take(40) ?: "(empty)",
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
            )

            StatusCard(
                title = "Monitor",
                value = when (state.monitorState) {
                    is ClipboardMonitorState.Idle -> "Idle"
                    is ClipboardMonitorState.Monitoring -> "Active"
                    is ClipboardMonitorState.Paused -> "Paused"
                    is ClipboardMonitorState.Error -> "Error"
                },
                color = when (state.monitorState) {
                    is ClipboardMonitorState.Monitoring -> MaterialTheme.colorScheme.primaryContainer
                    is ClipboardMonitorState.Paused -> MaterialTheme.colorScheme.tertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Pause/Resume
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.onAction(ClipboardAction.Pause) },
                modifier = Modifier.weight(1f),
                enabled = state.monitorState is ClipboardMonitorState.Monitoring,
            ) { Text("Pause") }

            OutlinedButton(
                onClick = { viewModel.onAction(ClipboardAction.Resume) },
                modifier = Modifier.weight(1f),
                enabled = state.monitorState is ClipboardMonitorState.Paused,
            ) { Text("Resume") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── URL Detections ──────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.urlDetections.isNotEmpty()) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("URL Detections (${state.urlDetections.size})", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                if (state.urlDetections.isEmpty()) {
                    Text("Copy a social media URL to detect", style = MaterialTheme.typography.bodySmall)
                } else {
                    state.urlDetections.take(5).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Clipboard History ───────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "History (${state.history.size}/${state.historyMaxSize})",
                style = MaterialTheme.typography.titleSmall,
            )
            if (state.history.isNotEmpty()) {
                OutlinedButton(onClick = { viewModel.onAction(ClipboardAction.ClearHistory) }) {
                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (state.history.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "Copy something to build history",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                )
            }
        } else {
            state.history.forEachIndexed { index, entry ->
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (index == 0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        },
                    ),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("#${index + 1}", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.content, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.contentType.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "COPY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { viewModel.onAction(ClipboardAction.CopyFromHistory(entry)) }
                                .padding(4.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "\u2715", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable { viewModel.onAction(ClipboardAction.RemoveFromHistory(entry)) }
                                .padding(4.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Info Card ───────────────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Architecture", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ClipboardViewModel (MVI) → ClipboardManager → " +
                        "Observer + Monitor + History. State survives config changes. " +
                        "All flows collected in viewModelScope.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
