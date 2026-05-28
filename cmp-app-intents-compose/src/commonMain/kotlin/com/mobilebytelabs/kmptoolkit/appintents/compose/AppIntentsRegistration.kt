/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.AppIntentsConfig
import com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi

/**
 * Lifecycle-bound AppIntents registration Composable.
 *
 * Calls [AppIntents.register] inside a [DisposableEffect] keyed by [config]. Re-registers when
 * config changes; intentionally does NOT deregister on disposal because the core API has no
 * `deregister()` at v0.4 (the in-memory registry stays alive for the app's lifetime — registration
 * is idempotent over identical configs).
 *
 * Place this Composable at the root of your Composable tree (typically in your App-level
 * Composable) so the registration happens once at first composition:
 *
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     val config = remember { appIntents { intent("openHome") { title = "Open"; perform { _ -> Done } } } }
 *     AppIntentsRegistration(config)
 *     // ... rest of your app
 * }
 * ```
 *
 * For non-Composable registration (e.g. Android `Application.onCreate`), use core
 * `AppIntents.register(config)` directly.
 */
@Composable
@ExperimentalAppIntentsApi
public fun AppIntentsRegistration(config: AppIntentsConfig) {
    DisposableEffect(config) {
        AppIntents.register(config)
        onDispose {
            // No-op — core API has no deregister() at v0.4; registry stays alive for app lifetime.
            // Re-registering an identical config is idempotent (AppIntentsRuntime.register overwrites).
        }
    }
}

/**
 * Memoized accessor for an [AppIntentsConfig] — calls [AppIntents.register] once at first
 * composition. Returns the config for downstream use (e.g. passing to `invokeForTesting`).
 *
 * Alternative shape of [AppIntentsRegistration] for cases where the consumer wants the config
 * handle threaded through the Composable tree.
 */
@Composable
@ExperimentalAppIntentsApi
public fun rememberRegisteredAppIntents(config: AppIntentsConfig): AppIntentsConfig =
    remember(config) {
        AppIntents.register(config)
        config
    }
