/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi::class)

package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.share

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.image
import com.mobilebytelabs.kmptoolkit.share.text
import com.mobilebytelabs.kmptoolkit.share.url
import kotlinx.coroutines.launch

@Composable
fun ShareDemoScreen(onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    DemoIntro(
        "Tap a button to invoke Share.share(...). The native share sheet appears on Android/iOS/macOS; navigator.share() on JS/wasmJs; xdg-open/cmd-start on Linux/mingw.",
    )

    Button(
        onClick = {
            scope.launch {
                val result = Share.text("Hello from KmpToolkit!", ShareOptions(chooserTitle = "Share text"))
                onStatus("Share.text → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Share text") }

    Button(
        onClick = {
            scope.launch {
                val result = Share.url(
                    "https://github.com/MobileByteLabs/KmpToolkit",
                    ShareOptions(chooserTitle = "Share URL"),
                )
                onStatus("Share.url → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Share URL") }

    OutlinedButton(
        onClick = {
            scope.launch {
                val pngBytes = byteArrayOf(
                    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                    0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4.toByte(), 0x89.toByte(),
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54,
                    0x78, 0x9C.toByte(), 0x62, 0x00, 0x01, 0x00, 0x00, 0x05,
                    0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4.toByte(), 0x00, 0x00,
                    0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte(),
                )
                val result = Share.image(pngBytes, "image/png", "demo.png")
                onStatus("Share.image → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Share image (1×1 PNG)") }
}
