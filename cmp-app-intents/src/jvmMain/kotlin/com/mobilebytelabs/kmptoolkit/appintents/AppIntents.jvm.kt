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
 * JVM Desktop `AppIntents` — no-op + `invokeForTesting` helper.
 *
 * Desktop has no native App Intents / Assistant surface. Registration stores the config
 * in the runtime registry so consumer code can manually invoke for testing / dev.
 *
 * For real PWA shortcut manifests on a JVM-hosted Web app, see future `cmp-pwa-shortcuts`.
 */
@ExperimentalAppIntentsApi
public actual object AppIntents {
    public actual fun register(config: AppIntentsConfig) {
        AppIntentsRuntime.register(config)
        // No OS surface to register against; consumer can use invokeForTesting.
    }

    public actual suspend fun invokeForTesting(id: String, params: Map<String, Any>): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)
}
