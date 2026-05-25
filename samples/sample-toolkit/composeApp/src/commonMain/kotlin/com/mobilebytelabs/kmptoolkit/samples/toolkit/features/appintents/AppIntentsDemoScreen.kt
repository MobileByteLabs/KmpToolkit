/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi::class)

package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.appintents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.ParamType
import com.mobilebytelabs.kmptoolkit.appintents.appIntents
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import kotlinx.coroutines.launch

@Composable
fun AppIntentsDemoScreen(onStatus: (String) -> Unit) {
    val scope = rememberCoroutineScope()

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

    DemoIntro(
        "Declarative App Intents DSL. iOS: writes manifest JSON for the CmpAppIntentBridge.swift consumer to read. Android: on-device registry. Desktop/Web: invokeForTesting helper for unit tests.",
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
