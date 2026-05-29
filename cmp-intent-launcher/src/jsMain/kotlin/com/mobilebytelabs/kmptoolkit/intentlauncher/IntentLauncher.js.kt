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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * JS browser `IntentLauncher` — creates a hidden `<input type=file>` for picker contracts.
 *
 * **HARD CONSTRAINT (Phase 0 TS6)**: `.launch()` MUST be invoked from within a user-gesture
 * call stack. Browsers reject programmatic `<input>.click()` outside a user-activation
 * context. Returns `IntentResult.Failed(IntentError.UserGestureMissing)` on browser block.
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        val contract = builder.resultContract

        return when (contract) {
            ResultContracts.PickImage,
            ResultContracts.PickDocument,
            ResultContracts.PickMultipleImages,
            is ResultContracts.Custom<*>,
            -> openFileInput(
                builder,
                multiple =
                contract == ResultContracts.PickMultipleImages,
            )

            else -> builder.onUnsupportedHandler?.invoke() ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private suspend fun openFileInput(builder: IntentBuilder, multiple: Boolean): IntentResult =
        suspendCancellableCoroutine { cont ->
            try {
                val accept = builder.type ?: "*/*"
                pickFile(accept, multiple) { uris ->
                    if (cont.isActive) {
                        if (uris.isEmpty()) {
                            cont.resume(IntentResult.Cancelled)
                        } else {
                            val data = IntentData(
                                uri = uris[0],
                                mimeType = builder.type,
                                extras = if (multiple) mapOf("uris" to uris.toList()) else emptyMap(),
                            )
                            cont.resume(IntentResult.Ok(data))
                        }
                    }
                }
            } catch (e: Throwable) {
                if (cont.isActive) cont.resume(classifyJsError(e))
            }
        }

    private fun classifyJsError(e: Throwable): IntentResult {
        val name = e.asDynamic().name as? String
        return when (name) {
            "NotAllowedError" -> IntentResult.Failed(IntentError.UserGestureMissing)
            else -> IntentResult.Failed(IntentError.Unknown(e.message ?: name ?: "JS launcher error"))
        }
    }
}

/**
 * Programmatically create + click a `<input type=file>` element; resolve with the
 * selected files' object URLs. Must be called within a user-gesture call stack.
 */
private fun pickFile(accept: String, multiple: Boolean, onResult: (Array<String>) -> Unit) {
    val input: dynamic = document.createElement("input")
    input.type = "file"
    input.accept = accept
    input.multiple = multiple
    input.style.display = "none"
    input.addEventListener("change") { event: dynamic ->
        val files = event.target.files
        val len = files.length as Int
        val urls = Array(len) { i ->
            objectUrlFor(files[i])
        }
        onResult(urls)
    }
    input.click()
}

private val document: dynamic get() = js("document")

private fun objectUrlFor(file: dynamic): String = js("URL.createObjectURL(file)") as String
