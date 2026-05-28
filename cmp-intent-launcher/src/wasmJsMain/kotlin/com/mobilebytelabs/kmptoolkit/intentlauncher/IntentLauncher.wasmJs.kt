/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.mobilebytelabs.kmptoolkit.intentlauncher

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * wasmJs `IntentLauncher` — v0.3 (inter-app-comms-real-native-impls Phase 3 T3):
 *
 * Picker contracts → `<input type=file>` via `@JsFun` external bindings.
 * **HARD CONSTRAINT (Phase 0 TS6)**: `.launch()` MUST be invoked within a user-gesture
 * call stack. The browser blocks `<input>.click()` outside gesture activation → we surface
 * as `IntentResult.Failed(IntentError.UserGestureMissing)` (NOT UnsupportedPlatform).
 *
 * Arbitrary intents → File System Access API path when `globalThis.window.showOpenFilePicker`
 * is defined (modern Chromium browsers); otherwise falls back to `<input type=file>`.
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
            -> openFileInput(
                accept = builder.type ?: "*/*",
                multiple = contract == ResultContracts.PickMultipleImages,
                mimeHint = builder.type,
            )
            // ADR-09: wasmJs has no canonical contact picker
            ResultContracts.PickContact -> IntentResult.Failed(IntentError.UnsupportedPlatform)
            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private suspend fun openFileInput(
        accept: String,
        multiple: Boolean,
        mimeHint: String?,
    ): IntentResult = suspendCancellableCoroutine { cont ->
        try {
            pickFileViaInput(accept, multiple) { firstUri: JsString? ->
                if (!cont.isActive) return@pickFileViaInput
                val uri = firstUri?.toString()
                if (uri.isNullOrBlank()) {
                    cont.resume(IntentResult.Cancelled)
                } else {
                    cont.resume(IntentResult.Ok(IntentData(uri = uri, mimeType = mimeHint)))
                }
            }
        } catch (t: Throwable) {
            // NotAllowedError = user-gesture violation
            val msg = t.message ?: "wasmJs picker error"
            if (msg.contains("NotAllowed", ignoreCase = true)) {
                cont.resume(IntentResult.Failed(IntentError.UserGestureMissing))
            } else {
                cont.resume(IntentResult.Failed(IntentError.Unknown(msg)))
            }
        }
    }
}

/**
 * Programmatically create + click a `<input type=file>` element; resolve with the first
 * selected file's object URL (or null on cancel). Must be called within a user-gesture
 * call stack per RULE-PROTO-CLICK-002 / Phase 0 TS6.
 */
@JsFun(
    """
    (accept, multiple, onResult) => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.multiple = multiple;
        input.style.display = 'none';
        input.addEventListener('change', e => {
            const f = e.target.files && e.target.files[0];
            onResult(f ? URL.createObjectURL(f) : null);
        }, { once: true });
        input.click();
    }
    """,
)
private external fun pickFileViaInput(
    accept: String,
    multiple: Boolean,
    onResult: (JsString?) -> Unit,
)
