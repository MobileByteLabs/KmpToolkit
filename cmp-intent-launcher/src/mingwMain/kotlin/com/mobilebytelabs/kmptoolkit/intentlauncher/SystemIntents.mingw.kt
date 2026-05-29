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

import platform.posix.system

/**
 * mingw (Windows) `SystemIntents` actual.
 *
 * - `openAppSettings()` — `start ms-settings:appsfeatures` via `system()` (canonical
 *   Windows-shell URI dispatch; works under MSYS / cmd alike).
 * - `createDocument()` — currently `UnsupportedPlatform`. Real `GetSaveFileNameW`
 *   cinterop is a follow-up; the existing `win32-pickers.def` only binds the OPEN
 *   variant. Tracked as a future v0.5 enhancement.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
@ExperimentalIntentLauncherApi
public actual object SystemIntents {

    public actual suspend fun openAppSettings(): IntentResult {
        val rc = system("start ms-settings:appsfeatures")
        return if (rc == 0) {
            IntentResult.Ok(IntentData(uri = "ms-settings:appsfeatures"))
        } else {
            IntentResult.Failed(IntentError.Unknown("system() exit=$rc"))
        }
    }

    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        IntentResult.Failed(IntentError.UnsupportedPlatform)
}
