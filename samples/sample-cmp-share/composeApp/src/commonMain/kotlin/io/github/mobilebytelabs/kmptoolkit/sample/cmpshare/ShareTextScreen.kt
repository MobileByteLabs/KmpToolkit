/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi::class)

package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareError
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import com.mobilebytelabs.kmptoolkit.share.text
import com.mobilebytelabs.kmptoolkit.share.url
import kotlinx.coroutines.launch

@Composable
fun ShareTextScreen() {
    val scope = rememberCoroutineScope()
    var textInput by remember { mutableStateOf("Hello from cmp-share — sent via the platform share sheet.") }
    var urlInput by remember { mutableStateOf("https://github.com/mobilebytelabs/kmptoolkit") }
    var lastResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Share text or URL",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Both demos call into Share.text(...) / Share.url(...) — the result reflects what the platform reported.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Text payload") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 4,
        )
        Button(
            onClick = {
                scope.launch {
                    lastResult = formatResult(Share.text(textInput))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Share text")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            label = { Text("URL payload") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
        )
        Button(
            onClick = {
                scope.launch {
                    lastResult = formatResult(Share.url(urlInput))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Share URL")
        }

        if (lastResult != null) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Last result",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lastResult ?: "",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}

private fun formatResult(result: ShareResult): String = when (result) {
    ShareResult.Completed -> "✅ Shared (platform reported completion)"
    ShareResult.Cancelled -> "↩ Cancelled by user"
    is ShareResult.Failed -> "❌ Failed: " + formatError(result.cause)
}

private fun formatError(err: ShareError): String = when (err) {
    ShareError.UnsupportedPlatform -> "Unsupported on this platform"
    ShareError.NoHandler -> "No handler available (no app accepted the payload)"
    ShareError.UserGestureMissing -> "User gesture missing (Web only — call from a click handler)"
    is ShareError.Unknown -> "Unknown — " + err.message
}
