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

/**
 * watchOS `SystemIntents` actual — `UnsupportedPlatform` for both entry points.
 * No programmatic settings deep-link; no document picker on watchOS. ADR-09 architectural.
 */
@ExperimentalIntentLauncherApi
public actual object SystemIntents {
    public actual suspend fun openAppSettings(): IntentResult = IntentResult.Failed(IntentError.UnsupportedPlatform)
    public actual suspend fun createDocument(suggestedName: String, mimeType: String): IntentResult =
        IntentResult.Failed(IntentError.UnsupportedPlatform)
}
