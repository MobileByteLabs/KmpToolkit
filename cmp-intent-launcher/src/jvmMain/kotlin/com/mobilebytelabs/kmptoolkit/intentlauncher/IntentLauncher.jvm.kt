/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

/**
 * JVM Desktop `IntentLauncher` — uses AWT `FileDialog` (LOAD mode) for picker contracts.
 *
 * - `ResultContracts.PickImage` / `PickDocument` → AWT FileDialog
 * - `ResultContracts.PickContact` → `Failed(UnsupportedPlatform)` (no JVM contact picker)
 * - Arbitrary actions → `onUnsupported` callback or `Failed(UnsupportedPlatform)`
 *
 * AWT FileDialog blocks the calling thread; we `withContext(Dispatchers.IO)`.
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher internal constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        val contract = builder.resultContract

        return when (contract) {
            ResultContracts.PickImage, ResultContracts.PickDocument -> openFileDialog(builder)

            is ResultContracts.Custom<*> -> openFileDialog(builder)

            // best-effort generic file pick
            else -> builder.onUnsupportedHandler?.invoke() ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private suspend fun openFileDialog(builder: IntentBuilder): IntentResult = withContext(Dispatchers.IO) {
        try {
            val dialog = FileDialog(null as Frame?, builder.action ?: "Pick a file", FileDialog.LOAD).apply {
                isVisible = true
            }
            val dir = dialog.directory ?: return@withContext IntentResult.Cancelled
            val file = dialog.file ?: return@withContext IntentResult.Cancelled
            val path = "file://$dir$file"
            IntentResult.Ok(IntentData(uri = path, mimeType = builder.type))
        } catch (e: Throwable) {
            IntentResult.Failed(IntentError.Unknown(e.message ?: "JVM FileDialog failed"))
        }
    }
}

@ExperimentalIntentLauncherApi
@Composable
public actual fun rememberIntentLauncher(): IntentLauncher = remember { IntentLauncher() }
