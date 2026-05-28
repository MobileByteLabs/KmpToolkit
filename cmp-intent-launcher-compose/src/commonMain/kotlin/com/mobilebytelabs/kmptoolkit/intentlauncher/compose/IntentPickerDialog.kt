/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher.compose

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentBuilder
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContract
import kotlinx.coroutines.launch

/**
 * Material 3 [AlertDialog] scaffold for launching an intent with a result contract.
 *
 * The dialog displays the contract name + description, with Confirm/Cancel buttons. On
 * Confirm, dispatches via [rememberIntentLauncher] and parses the result through the
 * contract before invoking [onResult].
 *
 * Example:
 * ```kotlin
 * var showPicker by remember { mutableStateOf(false) }
 * if (showPicker) {
 *     IntentPickerDialog(
 *         contract = ResultContracts.PickImage,
 *         onResult = { uri -> /* handle picked image URI */ },
 *         onDismiss = { showPicker = false },
 *     )
 * }
 * Button(onClick = { showPicker = true }) { Text("Pick image") }
 * ```
 *
 * @param contract the [ResultContract] determining which native picker to launch + how to parse the result
 * @param onResult called with the parsed contract result on user Confirm + successful pick
 * @param onDismiss called when the dialog is dismissed (Cancel, scrim tap, or after Confirm + result delivery)
 * @param title custom dialog title (default "Pick")
 * @param confirmLabel custom Confirm button label (default "Pick")
 * @param cancelLabel custom Cancel button label (default "Cancel")
 * @param additionalIntentConfig optional [IntentBuilder] DSL block for extra intent configuration (mime type, extras, etc.)
 */
@Composable
@ExperimentalIntentLauncherApi
public fun <R> IntentPickerDialog(
    contract: ResultContract<R>,
    onResult: (R) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pick",
    confirmLabel: String = "Pick",
    cancelLabel: String = "Cancel",
    additionalIntentConfig: IntentBuilder.() -> Unit = {},
) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("Select a ${contract::class.simpleName ?: "item"}") },
        confirmButton = {
            Button(onClick = {
                scope.launch {
                    val result = launcher.launch {
                        result(contract)
                        additionalIntentConfig()
                    }
                    if (result is IntentResult.Ok) {
                        @Suppress("UNCHECKED_CAST")
                        onResult(contract.parse(result.data))
                    }
                    onDismiss()
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        },
    )
}
