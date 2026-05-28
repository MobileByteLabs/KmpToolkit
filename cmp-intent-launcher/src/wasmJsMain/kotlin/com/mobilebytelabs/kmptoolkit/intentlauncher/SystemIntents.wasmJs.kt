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
 * wasmJs `SystemIntents` actual.
 *
 * - `openAppSettings()` — `UnsupportedPlatform` (browsers have no app-scoped settings).
 * - `createDocument()` — wraps `window.showSaveFilePicker()` via `@JsFun` external
 *   binding (mirrors the `pickFileViaInput` pattern in `IntentLauncher.wasmJs.kt`).
 *   Returns `UnsupportedPlatform` on Firefox / Safari (no File System Access API),
 *   `Cancelled` on user dismissal (AbortError / NotAllowedError).
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult = IntentResult.Failed(IntentError.UnsupportedPlatform)

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        suspendCancellableCoroutine { cont ->
            try {
                showSaveFilePickerWasm(
                    suggestedName = suggestedName,
                    mimeType = mimeType,
                ) { state: JsString, value: JsString? ->
                    if (!cont.isActive) return@showSaveFilePickerWasm
                    val s = state.toString()
                    val v = value?.toString()
                    cont.resume(
                        when (s) {
                            "ok" -> if (v.isNullOrBlank()) {
                                IntentResult.Failed(IntentError.Unknown("showSaveFilePicker returned blank name"))
                            } else {
                                IntentResult.Ok(IntentData(uri = "file-system-access:$v", mimeType = mimeType))
                            }
                            "cancelled" -> IntentResult.Cancelled
                            "unsupported" -> IntentResult.Failed(IntentError.UnsupportedPlatform)
                            else -> IntentResult.Failed(IntentError.Unknown(v ?: "showSaveFilePicker error"))
                        },
                    )
                }
            } catch (t: Throwable) {
                if (cont.isActive) {
                    cont.resume(IntentResult.Failed(IntentError.Unknown(t.message ?: "wasmJs save picker error")))
                }
            }
        }
}

/**
 * Single-call JS bridge for `window.showSaveFilePicker`. Reports via the callback
 * with a `state` discriminator ("ok" | "cancelled" | "unsupported" | "error") and a
 * `value` carrying the picked filename or the error message.
 */
@JsFun(
    """
    (suggestedName, mimeType, onResult) => {
        if (typeof window === 'undefined' || typeof window.showSaveFilePicker !== 'function') {
            onResult('unsupported', null);
            return;
        }
        var opts = { suggestedName: suggestedName };
        if (mimeType && mimeType !== '*/*') {
            opts.types = [{ description: 'Document', accept: { [mimeType]: [] } }];
        }
        window.showSaveFilePicker(opts).then(function(handle){
            onResult('ok', handle.name || suggestedName);
        }).catch(function(err){
            if (err && (err.name === 'AbortError' || err.name === 'NotAllowedError')) {
                onResult('cancelled', null);
            } else {
                onResult('error', (err && err.message) ? err.message : 'showSaveFilePicker error');
            }
        });
    }
    """,
)
private external fun showSaveFilePickerWasm(
    suggestedName: String,
    mimeType: String,
    onResult: (state: JsString, value: JsString?) -> Unit,
)
