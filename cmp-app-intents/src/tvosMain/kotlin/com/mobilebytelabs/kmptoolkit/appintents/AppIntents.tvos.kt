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
 * tvOS `AppIntents` — manifest JSON only; Siri Suggestions partial support.
 *
 * tvOS 14+ supports App Intents for Siri Suggestions on Apple TV, but lacks
 * `CoreSpotlight.framework` (no `CSSearchableIndex`). The Swift bridge skips
 * indexing on tvOS; the manifest JSON is still written so consumer Swift code
 * can register `AppShortcutsProvider` entries.
 *
 * Real implementation (Phase 10.C) — for v0.2 scaffolding this is registry-only.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
