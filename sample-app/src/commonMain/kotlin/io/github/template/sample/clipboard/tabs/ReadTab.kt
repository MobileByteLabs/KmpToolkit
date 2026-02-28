package io.github.template.sample.clipboard.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard
import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
import io.github.template.sample.appupdate.components.ConfigPreview

/**
 * Read Tab - Demonstrates reading from clipboard.
 *
 * Features:
 * - Read clipboard contents
 * - Check if clipboard has text
 * - Clear clipboard
 * - Platform support indicator
 *
 * Note: Reading is not supported on all platforms (JS/Wasm).
 */
@Composable
fun ReadTab() {
    var clipboardContent by remember { mutableStateOf<String?>(null) }
    var hasText by remember { mutableStateOf<Boolean?>(null) }
    var readError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Read from Clipboard",
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = "Read the current clipboard contents. Note: Reading may not be " +
                "supported on all platforms (e.g., Web browsers have restrictions).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    readError = null
                    clipboardContent = try {
                        getFromClipboard()
                    } catch (e: Exception) {
                        readError = e.message
                        null
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Read Clipboard")
            }

            OutlinedButton(
                onClick = {
                    hasText = hasClipboardText()
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Has Text?")
            }
        }

        OutlinedButton(
            onClick = {
                clearClipboard()
                clipboardContent = null
                hasText = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Clear Clipboard")
        }

        // Results display
        clipboardContent?.let { content ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Clipboard Contents:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (clipboardContent == null && readError == null && hasText != true) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Text(
                    text = "Clipboard is empty or reading is not supported on this platform.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        readError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336).copy(alpha = 0.1f),
                ),
            ) {
                Text(
                    text = "Error: $error",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFF44336),
                )
            }
        }

        hasText?.let { has ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (has) {
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                ),
            ) {
                Text(
                    text = if (has) "Clipboard has text content" else "Clipboard is empty",
                    modifier = Modifier.padding(16.dp),
                    color = if (has) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Code examples
        Text(
            text = "Read Clipboard",
            style = MaterialTheme.typography.titleSmall,
        )
        ConfigPreview(
            code = """
                import com.mobilebytelabs.kmptoolkit.clipboard.getFromClipboard

                // Read text from clipboard
                val text = getFromClipboard()

                text?.let { content ->
                    // Use the clipboard content
                    println("Clipboard: ${'$'}content")
                } ?: run {
                    // Clipboard is empty or not supported
                    println("No clipboard content")
                }
            """.trimIndent(),
        )

        Text(
            text = "Check & Clear",
            style = MaterialTheme.typography.titleSmall,
        )
        ConfigPreview(
            code = """
                import com.mobilebytelabs.kmptoolkit.clipboard.hasClipboardText
                import com.mobilebytelabs.kmptoolkit.clipboard.clearClipboard

                // Check if clipboard has text
                if (hasClipboardText()) {
                    showPasteButton()
                }

                // Clear clipboard (e.g., after pasting sensitive data)
                clearClipboard()
            """.trimIndent(),
        )
    }
}
