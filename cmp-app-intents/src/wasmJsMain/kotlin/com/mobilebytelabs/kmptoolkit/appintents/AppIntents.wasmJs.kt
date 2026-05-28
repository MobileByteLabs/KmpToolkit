/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

/**
 * wasmJs `AppIntents` — **ADR-09 #10 WONTFIX (architectural)** per v0.4 docs refresh.
 *
 * Same rationale as JS: Web has no canonical OS-level intent registration API. PWA shortcut
 * Web App Manifest action handlers deferred to potential future `cmp-pwa-shortcuts` module
 * per GOAL.md D8. `register()` is intentionally no-op; `invokeForTesting` works for dev/test.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
