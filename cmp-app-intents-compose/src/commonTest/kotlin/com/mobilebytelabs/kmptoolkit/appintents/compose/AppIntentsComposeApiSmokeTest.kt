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

import com.mobilebytelabs.kmptoolkit.appintents.AppIntentResult
import com.mobilebytelabs.kmptoolkit.appintents.AppIntents
import com.mobilebytelabs.kmptoolkit.appintents.ExperimentalAppIntentsApi
import com.mobilebytelabs.kmptoolkit.appintents.appIntents
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Smoke tests for cmp-app-intents-compose v1.0 public API.
 *
 * Earlier revision used `::AppIntentsRegistration` function references which threw
 * `NoClassDefFoundError: KComposableFunction1` at JVM-test runtime — the Compose
 * Compiler emits `KComposableFunction{N}` reference types that aren't on the JVM
 * test classpath without `androidx.compose.runtime:runtime` at a specific internal
 * artifact path. We just exercise the underlying imperative API instead.
 *
 * Full Composable-rendering tests require `compose-multiplatform-test` setup —
 * deferred to a post-v0.4 CI run.
 */
@OptIn(ExperimentalAppIntentsApi::class)
class AppIntentsComposeApiSmokeTest {

    @Test
    fun underlying_register_and_invoke_round_trip() = runBlocking {
        // Smoke: prove the imperative API the Composables wrap actually works.
        val config = appIntents {
            intent("smokeTestIntent") {
                title = "Smoke"
                description = "Smoke test"
                perform { _ -> AppIntentResult.Done }
            }
        }
        AppIntents.register(config)
        val result = AppIntents.invokeForTesting("smokeTestIntent", emptyMap())
        assertNotNull(result)
        assertEquals(AppIntentResult.Done, result)
    }
}
