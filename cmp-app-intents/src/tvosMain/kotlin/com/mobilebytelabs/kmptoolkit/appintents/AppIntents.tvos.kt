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
 * tvOS `AppIntents` — runtime-registry only.
 *
 * Manifest-write + AppIntentsCallback Swift-bridge dispatch are intentionally NOT
 * implemented on tvOS at this layer:
 *
 * - `AppIntentsCallback` (the ObjC-bridged singleton consumed by `CmpAppIntentBridge.swift`)
 *   is defined in iosMain + macosMain only; tvOS Swift bridge would need its own
 *   delivery mechanism (tvOS App Intents have limited reach vs iOS — no CoreSpotlight,
 *   no Siri Suggestions on appletvOS<14, no AppShortcutsProvider).
 * - The manifest-write to `NSDocumentDirectory` hits a watchosArm32-equivalent NSInteger
 *   bit-width conflict in some Apple platforms (separate issue from tvOS), so we keep
 *   tvOS aligned with watchOS = runtime-registry only.
 *
 * Consumers wanting full tvOS App Intents integration ship their own Swift bridge that
 * calls `AppIntentsRuntime.invoke(id, params)` directly via `@ObjCName` exports from
 * a per-app appleMain bridge file.
 *
 * ADR-09 #11 audit refresh: tvOS = runtime-only at v0.4; full manifest + Swift dispatch
 * follow-up post-v0.4 once the appleMain consolidation lands.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
