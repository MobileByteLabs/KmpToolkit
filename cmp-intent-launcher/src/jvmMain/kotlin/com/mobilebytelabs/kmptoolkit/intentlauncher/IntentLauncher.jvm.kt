/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: full

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.HeadlessException
import java.net.URI

/**
 * JVM Desktop `IntentLauncher`.
 *
 * - `action == "VIEW"` + `dataUri` → [Desktop.browse] (JDK-blessed since Java 6;
 *   opens the URL in the user's default browser/handler). Falls through to
 *   `Failed(NoHandler)` if Desktop API absent (headless JVM) or browse unsupported.
 * - `ResultContracts.PickImage` / `PickDocument` / `Custom<*>` → AWT `FileDialog` (LOAD).
 * - `ResultContracts.PickContact` → `Failed(UnsupportedPlatform)` (no JVM contact picker).
 * - Arbitrary actions → `onUnsupported` callback or `Failed(UnsupportedPlatform)`.
 *
 * AWT FileDialog blocks the calling thread; we `withContext(Dispatchers.IO)`.
 *
 * 2026-06-01 — Added `Desktop.browse()` branch for ACTION_VIEW URLs
 * (per cmp-intent-share-coverage-trueup sub-plan 02 T1).
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)

        // VIEW-action + data URI → Desktop.browse() (try first; falls through on absence/failure)
        if (builder.action == "VIEW" && !builder.data.isNullOrBlank()) {
            tryDesktopBrowse(builder.data!!)?.let { return it }
        }

        val contract = builder.resultContract
        return when (contract) {
            ResultContracts.PickImage, ResultContracts.PickDocument -> openFileDialog(builder)

            is ResultContracts.Custom<*> -> openFileDialog(builder)

            // best-effort generic file pick
            else -> builder.onUnsupportedHandler?.invoke() ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    /**
     * Open [uri] in the user's default browser via `java.awt.Desktop`. Returns:
     * - [IntentResult.Ok] on success
     * - `null` when Desktop API is absent / unsupported / headless — caller falls through
     * - [IntentResult.Failed] with [IntentError.Unknown] when browse() throws after starting
     */
    private suspend fun tryDesktopBrowse(uri: String): IntentResult? = withContext(Dispatchers.IO) {
        try {
            if (!Desktop.isDesktopSupported()) return@withContext null
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.BROWSE)) return@withContext null
            desktop.browse(URI(uri))
            IntentResult.Ok(IntentData(uri = uri, mimeType = null, extras = emptyMap()))
        } catch (_: HeadlessException) {
            null // Headless JVM — let caller fall through to fallback branches
        } catch (e: Throwable) {
            IntentResult.Failed(IntentError.Unknown(e.message ?: "Desktop.browse failed"))
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
