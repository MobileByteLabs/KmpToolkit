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
 * watchOS `AppIntents` — FULL impl via existing manifest + Swift bridge.
 *
 * watchOS 10+ supports App Intents natively (Shortcuts on Apple Watch). The same
 * `CmpAppIntentBridge.swift` works — manifest JSON written, Swift consumes it at
 * launch, `AppShortcutsProvider` registration works as on iOS.
 *
 * `CoreSpotlight.framework` is NOT available on watchOS; the Swift bridge skips
 * indexing via `#if canImport(CoreSpotlight)` guards.
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
