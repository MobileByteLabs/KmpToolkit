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
 * mingw (Windows) `AppIntents` — registry-only; no native Shortcuts equivalent.
 *
 * Phase 10.B will add manifest JSON write to `%APPDATA%\cmp-app-intents\manifest.json`
 * for external Windows tooling consumption (StartMenu shortcuts, AppX manifests).
 *
 * For now: registry-only. `invokeForTesting` works for dev/test.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
