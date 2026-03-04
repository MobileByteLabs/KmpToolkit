package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText

@Composable
fun ClipboardScreen() {
    var inputText by remember { mutableStateOf("") }
    var clipboardContent by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var hasText by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "KMP Clipboard Demo",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cross-platform clipboard utilities",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter text to copy") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val success = copyToClipboard(inputText)
                    statusMessage = if (success) {
                        "✓ Copied to clipboard"
                    } else {
                        "✗ Failed to copy"
                    }
                    hasText = hasClipboardText()
                },
                enabled = inputText.isNotEmpty()
            ) {
                Text("Copy")
            }

            Button(
                onClick = {
                    clipboardContent = getFromClipboard()
                    statusMessage = if (clipboardContent != null) {
                        "✓ Pasted from clipboard"
                    } else {
                        "✗ Clipboard empty or unavailable"
                    }
                    hasText = hasClipboardText()
                }
            ) {
                Text("Paste")
            }

            OutlinedButton(
                onClick = {
                    clearClipboard()
                    clipboardContent = null
                    statusMessage = "✓ Clipboard cleared"
                    hasText = hasClipboardText()
                }
            ) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                hasText = hasClipboardText()
                statusMessage = if (hasText) {
                    "Clipboard has text content"
                } else {
                    "Clipboard is empty"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Clipboard")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Status message
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusMessage.startsWith("✓")) {
                    MaterialTheme.colorScheme.primary
                } else if (statusMessage.startsWith("✗")) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clipboard content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Clipboard Content:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = clipboardContent ?: "(empty or not available)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (clipboardContent != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Platform info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Platform Info",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Running on: ${getPlatform().name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Has clipboard text: $hasText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
