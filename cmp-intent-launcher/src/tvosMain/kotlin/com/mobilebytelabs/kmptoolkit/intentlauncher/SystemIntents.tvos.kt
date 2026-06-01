/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
// LD-2-coverage: wontfix-OS

package com.mobilebytelabs.kmptoolkit.intentlauncher

/**
 * tvOS `SystemIntents` actual — `UnsupportedPlatform` for both entry points.
 * No app-scoped settings deep-link; no document picker UI on tvOS. ADR-09 architectural.
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {
    public actual suspend fun openAppSettings(): IntentResult = IntentResult.Failed(IntentError.UnsupportedPlatform)
    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        IntentResult.Failed(IntentError.UnsupportedPlatform)
}
