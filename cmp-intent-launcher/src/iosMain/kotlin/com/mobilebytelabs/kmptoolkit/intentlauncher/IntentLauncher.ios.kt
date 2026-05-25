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
 * iOS `IntentLauncher` — v0.1 routes picker contracts via `onUnsupported` callback path
 * until full `UIDocumentPickerViewController` / `PHPickerViewController` delegate plumbing
 * lands (sub-plan 07 instrumentation + v0.2 polish).
 *
 * **v0.1 behaviour**:
 * - `ResultContracts.PickImage` / `PickDocument` / `PickContact` → returns `IntentResult.Failed(UnsupportedPlatform)`
 *   UNLESS the caller provided `onUnsupported { ... }` in the builder, in which case that lambda fires.
 * - Custom actions → same — caller's `onUnsupported` is the only safe path on iOS in v0.1.
 *
 * **v0.2 plan**: per-contract delegate (UIDocumentPickerDelegate / PHPickerViewControllerDelegate) wired
 * into the Composable's remembered state; bridges via `suspendCancellableCoroutine`. Tracked as
 * Phase 0 follow-up FU-1 in `SPIKE_FINDINGS.md`.
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
