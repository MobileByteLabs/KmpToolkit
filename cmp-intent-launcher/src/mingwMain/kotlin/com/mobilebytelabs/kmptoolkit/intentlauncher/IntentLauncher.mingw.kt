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

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.system

/**
 * mingw (Windows) `IntentLauncher` — v0.3 (inter-app-comms-real-native-impls Phase 3 T7):
 *
 * - Arbitrary ACTION_VIEW → `cmd /c start "" "<url>"` (Windows shell resolver — http/https/
 *   mailto / file paths handled by registry-configured handler)
 * - Picker contracts → `UnsupportedPlatform` per ADR-09: Win32 `GetOpenFileNameW` cinterop
 *   (.def at `src/mingwMain/cinterop/win32-pickers.def`) is the v0.4 candidate; the cinterop
 *   pattern is proven by Phase 0 S0.A spike (win32-clipboard.def in cmp-share) but the
 *   `OPENFILENAMEW` struct marshaling adds non-trivial Kotlin/Native pointer work.
 */
@OptIn(ExperimentalForeignApi::class)
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        return when (builder.resultContract) {
            // ADR-09: Win32 GetOpenFileNameW cinterop deferred to v0.4
            ResultContracts.PickImage,
            ResultContracts.PickDocument,
            ResultContracts.PickMultipleImages,
            ResultContracts.PickContact -> IntentResult.Failed(IntentError.UnsupportedPlatform)
            null -> arbitraryUrl(builder)
            else -> builder.onUnsupportedHandler?.invoke()
                ?: IntentResult.Failed(IntentError.UnsupportedPlatform)
        }
    }

    private fun arbitraryUrl(builder: IntentBuilder): IntentResult {
        val uri = builder.data ?: return IntentResult.Failed(IntentError.NoHandler)
        val escaped = uri.replace("\"", "\\\"")
        val rc = system("cmd /c start \"\" \"$escaped\"")
        return if (rc == 0) IntentResult.Ok(IntentData(uri = uri, mimeType = builder.type)) else IntentResult.Failed(IntentError.NoHandler)
    }
}
