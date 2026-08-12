/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi::class)

package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.intentlauncher

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentError
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContracts
import com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import kotlinx.coroutines.launch

@Composable
fun IntentLauncherDemoScreen(onStatus: (String) -> Unit) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()

    DemoIntro(
        "Compose-scoped Intent launcher. Picker contracts work on Android + JVM + JS; iOS/macOS/wasmJs route through onUnsupported in v0.1.",
    )

    Button(
        onClick = {
            scope.launch {
                val result = launcher.launch {
                    result(ResultContracts.PickImage)
                    type("image/*")
                    onUnsupported { IntentResult.Failed(IntentError.UnsupportedPlatform) }
                }
                onStatus("Pick image → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Pick image") }

    Button(
        onClick = {
            scope.launch {
                val result = launcher.launch {
                    result(ResultContracts.PickDocument)
                    type("application/pdf")
                    onUnsupported { IntentResult.Failed(IntentError.UnsupportedPlatform) }
                }
                onStatus("Pick PDF → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Pick PDF document") }
}
