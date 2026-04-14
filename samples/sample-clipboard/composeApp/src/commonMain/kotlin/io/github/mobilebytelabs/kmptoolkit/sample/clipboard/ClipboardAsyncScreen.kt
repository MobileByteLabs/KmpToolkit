package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboardAsync
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboardAsync
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardTextAsync
import kotlinx.coroutines.launch

@Composable
fun ClipboardAsyncScreen() {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var asyncResult by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Async Clipboard Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Suspend functions — works on JS/Wasm too",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Quick Copy Samples ──────────────────────────────────
        Text(
            text = "Tap to Async Copy + Auto Read",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val samples = listOf(
            "Hello from Async API!",
            "https://www.instagram.com/reel/ABC123/",
            "Kotlin Multiplatform rocks!",
            "https://youtu.be/dQw4w9WgXcQ",
        )

        samples.forEach { text ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            isLoading = true
                            val success = copyToClipboardAsync(text)
                            statusMessage = if (success) "Copied: ${text.take(30)}" else "Copy failed"
                            // Auto-read back to verify
                            asyncResult = getFromClipboardAsync()
                            isLoading = false
                        }
                    },
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
                        text = "TAP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Custom Input ────────────────────────────────────────
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Or enter custom text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val success = copyToClipboardAsync(inputText)
                        statusMessage = if (success) "Async copy success" else "Async copy failed"
                        asyncResult = getFromClipboardAsync()
                        isLoading = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = inputText.isNotEmpty() && !isLoading,
            ) {
                Text("Copy")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        asyncResult = getFromClipboardAsync()
                        statusMessage = if (asyncResult != null) "Read success" else "Empty"
                        isLoading = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
            ) {
                Text("Read")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val has = hasClipboardTextAsync()
                        statusMessage = if (has) "Has text" else "Empty"
                        isLoading = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading,
            ) {
                Text("Check")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Status ──────────────────────────────────────────────
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Result Card ─────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Async Read Result", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = asyncResult ?: "(tap a sample or use Read button)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (asyncResult != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Info Card ───────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Why Async?", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "On JS/Wasm, clipboard read is only possible via the async " +
                        "navigator.clipboard API. The sync getFromClipboard() returns null " +
                        "on those platforms. The async versions work everywhere.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
