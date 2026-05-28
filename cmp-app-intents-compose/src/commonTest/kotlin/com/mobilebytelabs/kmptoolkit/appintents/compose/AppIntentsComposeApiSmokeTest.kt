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
import kotlin.test.assertTrue

/**
 * Smoke tests for cmp-app-intents-compose v1.0 public API.
 *
 * NOTE: full Composable rendering tests require `compose-multiplatform-test` setup (deferred
 * to post-v0.4 CI). These smoke tests cover the type signatures + verify that the underlying
 * `AppIntents.register` + `invokeForTesting` round-trip works (proves the Composables wrap
 * a working imperative API).
 */
@OptIn(ExperimentalAppIntentsApi::class)
class AppIntentsComposeApiSmokeTest {

    @Test
    fun appIntentsRegistration_signature_resolves() {
        val ref = ::AppIntentsRegistration
        assertTrue(ref.name == "AppIntentsRegistration")
    }

    @Test
    fun appIntentsRegistry_signature_resolves() {
        val ref = ::AppIntentsRegistry
        assertTrue(ref.name == "AppIntentsRegistry")
    }

    @Test
    fun rememberRegisteredAppIntents_signature_resolves() {
        val ref = ::rememberRegisteredAppIntents
        assertTrue(ref.name == "rememberRegisteredAppIntents")
    }

    @Test
    fun underlying_register_and_invoke_round_trip() = runBlocking {
        // Smoke: prove the underlying imperative API the Composables wrap actually works.
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
