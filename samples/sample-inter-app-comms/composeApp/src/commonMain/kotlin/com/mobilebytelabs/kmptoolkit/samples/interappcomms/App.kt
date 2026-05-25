/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(
    com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi::class,
    com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi::class,
    com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi::class,
)

package com.mobilebytelabs.kmptoolkit.samples.interappcomms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.ParamType
import com.mobilebytelabs.kmptoolkit.appintents.appIntents
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContracts
import com.mobilebytelabs.kmptoolkit.intentlauncher.rememberIntentLauncher
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import com.mobilebytelabs.kmptoolkit.share.image
import com.mobilebytelabs.kmptoolkit.share.text
import com.mobilebytelabs.kmptoolkit.share.url
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleInterAppCommsApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            var tab by remember { mutableStateOf(0) }
            var status by remember { mutableStateOf("Ready") }

            Scaffold(
                topBar = { TopAppBar(title = { Text("Inter-App Comms — kmp-toolkit") }) },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Share") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Intent") })
                        Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("AppIntents") })
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (tab) {
                            0 -> ShareTab(onStatus = { status = it })
                            1 -> IntentTab(onStatus = { status = it })
                            2 -> AppIntentsTab(onStatus = { status = it })
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Status: $status", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTab(onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    Text("cmp-share — OS-native share sheet", style = MaterialTheme.typography.titleMedium)
    Text("Tap a button below to invoke Share.share(...).", style = MaterialTheme.typography.bodySmall)

    Button(
        onClick = {
            scope.launch {
                val result = Share.text("Hello from cmp-share!", ShareOptions(chooserTitle = "Share text"))
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
                // Tiny 1x1 transparent PNG bytes for demo
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

@Composable
private fun IntentTab(onStatus: (String) -> Unit) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()

    Text("cmp-intent-launcher — typed Intent with ActivityResult", style = MaterialTheme.typography.titleMedium)
    Text(
        "Compose-scoped launcher. Picker contracts work on Android + JVM + JS; iOS/macOS/wasmJs route through onUnsupported in v0.1.",
        style = MaterialTheme.typography.bodySmall,
    )

    Button(
        onClick = {
            scope.launch {
                val result = launcher.launch {
                    result(ResultContracts.PickImage)
                    type("image/*")
                    onUnsupported {
                        IntentResult.Failed(
                            com.mobilebytelabs.kmptoolkit.intentlauncher.IntentError.UnsupportedPlatform,
                        )
                    }
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
                    onUnsupported {
                        IntentResult.Failed(
                            com.mobilebytelabs.kmptoolkit.intentlauncher.IntentError.UnsupportedPlatform,
                        )
                    }
                }
                onStatus("Pick PDF → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Pick PDF document") }
}

@Composable
private fun AppIntentsTab(onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()

    // Register intents once per app launch
    LaunchedEffect(Unit) {
        AppIntents.register(
            appIntents {
                intent("openHome") {
                    title = "Open Home"
                    description = "Opens the Home tab"
                    searchable(category = "Navigation")
                    perform { _ -> AppIntentResult.Dialog("Opened Home tab") }
                }
                intent("greet") {
                    title = "Greet"
                    description = "Greets the user by name"
                    parameter("name", ParamType.Text)
                    searchable()
                    perform { params -> AppIntentResult.Dialog("Hello, ${params["name"]}!") }
                }
            },
        )
        onStatus("Registered 2 intents (openHome, greet)")
    }

    Text("cmp-app-intents — declarative App Intents DSL", style = MaterialTheme.typography.titleMedium)
    Text(
        "iOS: writes manifest JSON for the CmpAppIntentBridge.swift consumer file to read. " +
            "Android: on-device registry (Assistant integration deferred to v0.2). " +
            "Desktop/Web: no-op + invokeForTesting helper.",
        style = MaterialTheme.typography.bodySmall,
    )

    Button(
        onClick = {
            scope.launch {
                val result = AppIntents.invokeForTesting("openHome")
                onStatus("Test invoke openHome → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Test-invoke openHome") }

    Button(
        onClick = {
            scope.launch {
                val result = AppIntents.invokeForTesting("greet", mapOf("name" to "World"))
                onStatus("Test invoke greet(name=World) → $result")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Test-invoke greet(name=\"World\")") }
}
