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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentBuilder
import com.mobilebytelabs.kmptoolkit.intentlauncher.IntentResult
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContract
import kotlinx.coroutines.launch

/**
 * Material 3 [ModalBottomSheet] variant of [IntentPickerDialog].
 *
 * Same launch semantics as the dialog, but presented as a swipe-dismissible bottom sheet
 * (better for tablet / wider-screen UX, or for flows where the picker is one step in a
 * multi-step task).
 *
 * Example:
 * ```kotlin
 * var showSheet by remember { mutableStateOf(false) }
 * if (showSheet) {
 *     IntentPickerSheet(
 *         contract = ResultContracts.PickDocument,
 *         onResult = { uri -> /* handle picked document */ },
 *         onDismiss = { showSheet = false },
 *     )
 * }
 * ```
 *
 * @param contract the [ResultContract] determining which native picker to launch
 * @param onResult called with the parsed contract result on successful pick
 * @param onDismiss called when the sheet is dismissed
 * @param sheetState the [SheetState] controlling sheet expansion behavior
 * @param title sheet title (default "Pick")
 * @param actionLabel action button label (default "Pick")
 * @param additionalIntentConfig optional [IntentBuilder] DSL block for extra intent config
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@ExperimentalIntentLauncherApi
public fun <R> IntentPickerSheet(
    contract: ResultContract<R>,
    onResult: (R) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    title: String = "Pick",
    actionLabel: String = "Pick",
    additionalIntentConfig: IntentBuilder.() -> Unit = {},
) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select a ${contract::class.simpleName ?: "item"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
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
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(actionLabel) }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
