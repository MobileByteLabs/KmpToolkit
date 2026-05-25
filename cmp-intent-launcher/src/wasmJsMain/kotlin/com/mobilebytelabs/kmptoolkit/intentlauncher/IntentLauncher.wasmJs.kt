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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * wasmJs `IntentLauncher` — v0.1 routes picker contracts via `onUnsupported` callback path.
 *
 * Real `<input type=file>` integration requires DOM bridging that's identical-in-spirit to
 * the JS path but cannot share code (wasmJs has no `dynamic`, must use `@JsFun` bindings
 * with explicit signatures). Deferred to sub-plan 07 polish.
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
