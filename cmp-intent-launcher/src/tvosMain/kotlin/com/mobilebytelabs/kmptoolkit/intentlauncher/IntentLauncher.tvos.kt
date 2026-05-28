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

/**
 * tvOS `IntentLauncher` — v0.3 Phase 1 stub. Real impl lands in Phase 3 of
 * inter-app-comms-real-native-impls (subset of ResultContracts supported on tvOS;
 * everything else exits via UnsupportedPlatform anchored to ADR-09).
 */
@ExperimentalIntentLauncherApi
public actual class IntentLauncher public constructor() {
    public actual suspend fun launch(block: IntentBuilder.() -> Unit): IntentResult {
        val builder = IntentBuilder().apply(block)
        builder.onUnsupportedHandler?.let { return it.invoke() }
        return IntentResult.Failed(IntentError.UnsupportedPlatform)
    }
}
