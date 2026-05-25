/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.clipboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.clipboard.ClipboardManager
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro

@Composable
fun ClipboardDemoScreen(onStatus: (String) -> Unit) {
    val clipboard = remember { ClipboardManager() }
    var text by remember { mutableStateOf("Hello from KmpToolkit clipboard!") }

    DemoIntro(
        "Sync copy/paste via ClipboardManager. Also supports observation, URL detection, and history (see API docs).",
    )

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Text to copy") },
        singleLine = true,
    )

    Button(
        onClick = {
            val ok = clipboard.copy(text)
            onStatus("copy(\"$text\") → $ok")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Copy to clipboard") }

    OutlinedButton(
        onClick = {
            val pasted = clipboard.paste() ?: "(empty)"
            onStatus("paste() → $pasted")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Paste from clipboard") }

    OutlinedButton(
        onClick = {
            val has = clipboard.hasText()
            onStatus("hasText() → $has")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Check hasText()") }
}
