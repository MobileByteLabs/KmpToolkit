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
 * watchOS `AppIntents` — runtime-registry only.
 *
 * Manifest-write + AppIntentsCallback Swift-bridge dispatch are intentionally NOT
 * implemented on watchOS at this layer:
 *
 * - `AppIntentsCallback` (the ObjC-bridged singleton consumed by `CmpAppIntentBridge.swift`)
 *   is defined in iosMain + macosMain only.
 * - `NSFileManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, ...)` triggers
 *   K/N expect-actual bit-width errors when `watchosArm32` is one of the declared targets
 *   (NSUInteger is 32-bit on armv7k / 64-bit elsewhere). Keeping watchOS at runtime-only
 *   sidesteps the issue without dropping the watchosArm32 target.
 *
 * Consumers wanting full watchOS App Intents integration ship their own Swift bridge that
 * calls `AppIntentsRuntime.invoke(id, params)` directly via `@ObjCName` exports from
 * a per-app appleMain bridge file. The runtime-registry side still works — `register()`
 * captures intents in memory + `invokeForTesting` is callable for in-app verification.
 *
 * ADR-09 #11 audit refresh: watchOS = runtime-only at v0.4; full manifest + Swift dispatch
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
