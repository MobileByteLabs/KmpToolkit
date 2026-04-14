package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .padding(16.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Enter text to copy async") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Async Copy
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val success = copyToClipboardAsync(inputText)
                    statusMessage = if (success) "Async copy success" else "Async copy failed"
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = inputText.isNotEmpty() && !isLoading,
        ) {
            Text(if (isLoading) "Copying..." else "Copy (Async)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Async Read
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    asyncResult = getFromClipboardAsync()
                    statusMessage = if (asyncResult != null) "Async read success" else "Clipboard empty or permission denied"
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            Text(if (isLoading) "Reading..." else "Read (Async)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Async Check
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    val hasText = hasClipboardTextAsync()
                    statusMessage = if (hasText) "Clipboard has text" else "Clipboard empty"
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
        ) {
            Text("Check (Async)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status
        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Async Read Result",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = asyncResult ?: "(no result yet)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (asyncResult != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Why Async?",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "On JS/Wasm, clipboard read is only possible via the async " +
                        "navigator.clipboard API. The sync getFromClipboard() returns null " +
                        "on these platforms. The async versions work everywhere.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
