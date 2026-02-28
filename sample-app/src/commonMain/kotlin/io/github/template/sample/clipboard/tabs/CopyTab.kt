package io.github.template.sample.clipboard.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard
import io.github.template.sample.appupdate.components.ConfigPreview

/**
 * Copy Tab - Demonstrates copying text to clipboard.
 *
 * Features:
 * - Text input field for custom text
 * - Copy button with success/error feedback
 * - Usage code example for developers
 */
@Composable
fun CopyTab() {
    var text by remember { mutableStateOf("Hello from KMP Toolkit!") }
    var status by remember { mutableStateOf<CopyStatus?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Copy Text to Clipboard",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Enter any text below and copy it to the system clipboard. " +
                "Works on all platforms!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Text to copy") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Button(
            onClick = {
                val success = copyToClipboard(text)
                status = if (success) {
                    CopyStatus.Success("Copied to clipboard!")
                } else {
                    CopyStatus.Error("Failed to copy")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Copy to Clipboard")
        }

        // Status feedback
        status?.let { currentStatus ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (currentStatus) {
                        is CopyStatus.Success -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        is CopyStatus.Error -> Color(0xFFF44336).copy(alpha = 0.1f)
                    },
                ),
            ) {
                Text(
                    text = when (currentStatus) {
                        is CopyStatus.Success -> currentStatus.message
                        is CopyStatus.Error -> currentStatus.message
                    },
                    modifier = Modifier.padding(16.dp),
                    color = when (currentStatus) {
                        is CopyStatus.Success -> Color(0xFF4CAF50)
                        is CopyStatus.Error -> Color(0xFFF44336)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Code example
        Text(
            text = "Usage Example",
            style = MaterialTheme.typography.titleSmall,
        )
        ConfigPreview(
            code = """
                import com.mobilebytelabs.kmptoolkit.clipboard.copyToClipboard

                // Copy text to clipboard
                val success = copyToClipboard("Hello, World!")

                if (success) {
                    // Text was copied successfully
                    showMessage("Copied!")
                } else {
                    // Copy failed (rare, usually on unsupported platforms)
                    showError("Failed to copy")
                }
            """.trimIndent(),
        )

        // Quick copy examples
        Text(
            text = "Quick Copy Examples",
            style = MaterialTheme.typography.titleMedium,
        )

        QuickCopyButton("Copy URL") {
            copyToClipboard("https://github.com/anthropics/claude-code")
        }

        QuickCopyButton("Copy Email") {
            copyToClipboard("hello@example.com")
        }

        QuickCopyButton("Copy JSON") {
            copyToClipboard("""{"name": "KMP Toolkit", "version": "1.0.0"}""")
        }
    }
}

@Composable
private fun QuickCopyButton(label: String, onCopy: () -> Boolean) {
    var copied by remember { mutableStateOf(false) }

    Button(
        onClick = {
            copied = onCopy()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (copied) "$label - Copied!" else label)
    }
}

private sealed class CopyStatus {
    data class Success(val message: String) : CopyStatus()
    data class Error(val message: String) : CopyStatus()
}
