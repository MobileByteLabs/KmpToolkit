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

/**
 * macOS `IntentLauncher` — v0.1 routes picker contracts via `onUnsupported` until
 * `NSOpenPanel` / `CNContactPicker` integration lands (sub-plan 07 + v0.2 polish).
 *
 * Same shape as iOS [IntentLauncher] — see iosMain KDoc for full rationale.
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher internal constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        builder.onUnsupportedHandler?.let { return it.invoke() }
        return IntentResult.Failed(IntentError.UnsupportedPlatform)
    }
}

@ExperimentalIntentLauncherApi
@Composable
public actual fun rememberIntentLauncher(): IntentLauncher = remember { IntentLauncher() }
