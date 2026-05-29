/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import kotlinx.coroutines.launch

/**
 * Material 3 styled bottom-sheet share UX. Displays a payload preview + Share button that
 * invokes [Share.share] on click. Dismisses via swipe-down or the dismiss button.
 *
 * Example:
 * ```kotlin
 * var showSheet by remember { mutableStateOf(false) }
 * if (showSheet) {
 *     ShareSheet(
 *         payload = SharePayload.Url("https://example.com"),
 *         onDismiss = { showSheet = false },
 *     )
 * }
 * Button(onClick = { showSheet = true }) { Text("Share link") }
 * ```
 *
 * @param payload the [SharePayload] to dispatch on share-button click
 * @param onDismiss called when the sheet is dismissed (swipe-down, scrim tap, or after successful share)
 * @param options optional [ShareOptions] forwarded to the underlying [Share.share] call
 * @param sheetState the [SheetState] controlling the sheet's expansion behavior
 * @param onResult called with the [ShareResult] after the share dispatch completes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@ExperimentalShareApi
public fun ShareSheet(
    payload: SharePayload,
    onDismiss: () -> Unit,
    options: ShareOptions = ShareOptions(),
    sheetState: SheetState = rememberModalBottomSheetState(),
    onResult: (ShareResult) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Share", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = payload.previewText(), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        val result = Share.share(payload, options)
                        onResult(result)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(modifier = Modifier.height(0.dp))
                Text("  Share")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Short human-readable preview for a [SharePayload] — used in [ShareSheet] body. */
private fun SharePayload.previewText(): String = when (this) {
    is SharePayload.Text -> "Text: ${content.take(80)}${if (content.length > 80) "…" else ""}"
    is SharePayload.Url -> "URL: $href"
    is SharePayload.Image -> "Image (${bytes.size} bytes, $mimeType${filename?.let { ", $it" } ?: ""})"
    is SharePayload.File -> "File: $uri ($mimeType)"
    is SharePayload.Multi -> "${items.size} items"
}
