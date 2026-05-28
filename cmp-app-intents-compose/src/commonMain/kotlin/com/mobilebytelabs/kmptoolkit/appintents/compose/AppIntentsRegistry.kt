/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentDef
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsRuntime
import com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi
import kotlinx.coroutines.launch

/**
 * Material 3 dev/debug UI showing all currently-registered AppIntents with per-row "Invoke" buttons
 * that call [AppIntents.invokeForTesting] for in-app verification.
 *
 * Read-only access to the [AppIntentsRuntime] runtime registry — does not modify registration state.
 *
 * Intended for dev/debug screens, NOT production UX. Production consumers wanting to surface
 * AppIntents to end-users should use the per-platform OS surfaces (Siri Shortcuts on iOS,
 * Google Assistant on Android via the generated shortcuts.xml).
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun DebugScreen() {
 *     AppIntentsRegistration(config)
 *     AppIntentsRegistry(modifier = Modifier.fillMaxSize())
 * }
 * ```
 */
@Composable
@ExperimentalAppIntentsApi
public fun AppIntentsRegistry(
    modifier: Modifier = Modifier,
    onInvokeResult: (intentId: String, result: AppIntentResult?) -> Unit = { _, _ -> },
) {
    val intents: List<AppIntentDef> = AppIntentsRuntime.current()?.intents ?: emptyList()
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (intents.isEmpty()) {
            item {
                Text(
                    "No AppIntents registered. Call AppIntents.register(config) or use AppIntentsRegistration().",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(intents, key = { it.id }) { intent ->
                IntentRow(
                    intent = intent,
                    onInvoke = {
                        scope.launch {
                            val result = AppIntents.invokeForTesting(intent.id, emptyMap())
                            onInvokeResult(intent.id, result)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
@ExperimentalAppIntentsApi
private fun IntentRow(intent: AppIntentDef, onInvoke: () -> Unit) {
    var lastResultLabel by remember { mutableStateOf<String?>(null) }
    ListItem(
        headlineContent = { Text(intent.title.ifBlank { intent.id }) },
        supportingContent = {
            Text(
                buildString {
                    append(intent.description)
                    if (intent.parameters.isNotEmpty()) {
                        append(" • params: ${intent.parameters.joinToString { it.name }}")
                    }
                    lastResultLabel?.let { append("\nLast: $it") }
                },
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Button(onClick = {
                onInvoke()
                lastResultLabel = "invoked"
            }) { Text("Invoke") }
        },
    )
}
