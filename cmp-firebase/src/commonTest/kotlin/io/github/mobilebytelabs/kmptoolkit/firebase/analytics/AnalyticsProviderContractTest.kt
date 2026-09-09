/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Tier-agnostic contract for [provideAnalyticsHelper], asserted on EVERY target —
 * including the browser (`jsBrowserTest` / `wasmJsBrowserTest`), where the firebase
 * tier is live.
 *
 * The contract is stated in `FirebaseKit.initialize`'s KDoc:
 *
 * > "When this platform has no options (and is not on the MP tier), analytics
 * > degrades to NoOp with a WARN — **it never throws** (analytics must not break
 * > the app)."
 *
 * `nonFirebaseMain` has always honoured this (`?: NoOpAnalyticsHelper`).
 * `firebaseMain` did not: it called `FirebaseAnalyticsHelper(Firebase.analytics)`
 * unconditionally, so a web/wasmJs app that never supplied `FirebaseConfig.web`
 * threw `No Firebase App '[DEFAULT]' has been created` from the analytics factory —
 * crashing the app on the very path the library logs as "analytics will NoOp".
 *
 * These tests pin the contract so the crash cannot come back on any tier.
 */
class AnalyticsProviderContractTest {

    @BeforeTest
    fun clearCache() {
        FirebaseRuntime.analyticsHelper = null
    }

    @AfterTest
    fun reset() {
        FirebaseRuntime.config = null
        FirebaseRuntime.analyticsHelper = null
    }

    /**
     * No config at all — the factory must return a usable helper rather than throw.
     * On the firebase tier this is the "Firebase was never initialized" path.
     */
    @Test
    fun provider_does_not_throw_when_no_config() {
        FirebaseRuntime.config = null
        assertNotNull(provideAnalyticsHelper())
    }

    /**
     * Config present but carrying no options for THIS platform and no MpConfig —
     * the documented degrade-to-NoOp case.
     */
    @Test
    fun provider_does_not_throw_when_config_has_no_options_for_platform() {
        FirebaseRuntime.config = FirebaseConfig()
        assertNotNull(provideAnalyticsHelper())
    }

    /**
     * The factory must always hand back a usable helper — never null, never a partially
     * constructed one — and calling it twice must return the SAME memoized instance so the
     * app's DI and the crash→GA4 bridge share one consent state.
     *
     * Deliberately NOT `assertSame(NoOpAnalyticsHelper, ...)`: which instance you get when
     * nothing is configured is tier- and platform-specific, and asserting identity here was
     * wrong. On JS/wasmJs `Firebase.analytics` throws for an uninitialized app, so the guard
     * in `AnalyticsProvider.firebase.kt` catches it and yields [NoOpAnalyticsHelper]. On
     * Apple the same accessor does NOT throw — it returns a live object bound to an
     * unconfigured Firebase — so a real [FirebaseAnalyticsHelper] comes back and simply
     * captures nothing. Both satisfy the documented contract ("degrades … it never throws");
     * only the identity differs. `provideAnalyticsHelper` on the non-Firebase tier is where
     * NoOp is genuinely contractual, and `AnalyticsTierRoutingTest` pins it there.
     */
    @Test
    fun unconfigured_platform_yields_a_stable_usable_helper() {
        FirebaseRuntime.config = FirebaseConfig()
        val first = provideAnalyticsHelper()
        assertNotNull(first)
        assertSame(first, provideAnalyticsHelper(), "helper must be memoized process-wide")
    }

    /**
     * Logging an event through the degraded helper must also not throw — the app
     * keeps running even though nothing is captured.
     */
    @Test
    fun degraded_helper_swallows_events_without_throwing() {
        FirebaseRuntime.config = FirebaseConfig()
        val helper = provideAnalyticsHelper()
        helper.logEvent("contract_probe", mapOf("k" to "v"))
        assertNotNull(helper)
    }
}
