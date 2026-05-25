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
 * Android `AppIntents` — v0.1 = on-device runtime registry only (per ADR-05).
 *
 * Google Assistant integration deferred to v0.2 `cmp-app-intents-assistant`.
 *
 * Test invocation:
 * ```
 * adb shell am broadcast -a cmp.appintents.INVOKE -e intent_id openTransfer --es amount 100
 * ```
 * (BroadcastReceiver auto-registers per consumer-side wiring in their Application class —
 * full receiver impl lands in sub-plan 06 follow-up; v0.1 here covers the runtime registry
 * + invokeForTesting helper.)
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
