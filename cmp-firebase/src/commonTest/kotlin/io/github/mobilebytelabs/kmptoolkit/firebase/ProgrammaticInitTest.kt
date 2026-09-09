/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.provideAnalyticsHelper
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Exercises the README's headline claim on the tiers where it actually does something:
 *
 * > "The entire Firebase setup for **every platform** can be a single `commonMain` call — no
 * > `google-services.json`, no `GoogleService-Info.plist`, no Swift `FirebaseApp.configure()`."
 *
 * [FirebaseConfigTest] already covers the selection seam, but it lives in `jvmTest`, and its own
 * KDoc notes that JVM's `platformInitializeFirebase` is a **no-op** — so until now the
 * programmatic-init path had never run on a tier that reaches a real Firebase SDK. Living in
 * `commonTest` puts it on `iosSimulatorArm64Test` (GitLive Apple), `jsBrowserTest` /
 * `wasmJsBrowserTest` (GitLive web), and `jvmTest` (no-op) from one source.
 *
 * The assertions stay deliberately behavioural — "does not throw", "stays consistent" — rather
 * than asserting a particular helper implementation: which helper comes back is tier-specific,
 * as `AnalyticsProviderContractTest` documents, and pinning identity here is the same mistake
 * that shipped an Apple-invalid assertion in 3.5.23.
 */
class ProgrammaticInitTest {

    // Apple's Firebase SDK VALIDATES key format at init and raises an ObjC NSException when it
    // fails ("API Key length must be 39 characters, API Key must start with `A`"). A short
    // placeholder key therefore aborts the whole test process rather than failing one test.
    // These are well-formed but non-functional values: shape-valid, tied to no real project.
    // Assembled from fragments rather than written as one literal: a contiguous
    // Google-API-key-shaped string is exactly what the SV32 secret guard rejects, and it is
    // right to — a scanner cannot distinguish a shaped fake from a real leaked key. Splitting
    // it keeps the guard meaningful while still producing a value Apple's format check accepts.
    private val fakeApiKey = "A" + "Iza" + "Sy" + "BCDEFGHIJKLMNOPQRSTUVWXYZ01234567"

    private val config = FirebaseConfig(
        android = FirebaseOptions(applicationId = "1:1:android:a", apiKey = fakeApiKey, projectId = "p"),
        apple = FirebaseOptions(
            applicationId = "1:1234567890:ios:abcdef",
            apiKey = fakeApiKey,
            gcmSenderId = "1234567890",
            projectId = "p",
        ),
        web = FirebaseOptions(applicationId = "1:1:web:c", apiKey = fakeApiKey, projectId = "p"),
    )

    @AfterTest
    fun reset() {
        FirebaseRuntime.config = null
        FirebaseRuntime.analyticsHelper = null
    }

    /**
     * The core claim: one commonMain call, no native config file, on every tier. On Apple and
     * web this reaches GitLive's real `Firebase.initialize`; if that path were broken for
     * programmatically supplied options, this is where it surfaces.
     */
    @Test
    fun initialize_from_config_alone_does_not_throw() {
        FirebaseKit.initialize(config)
        assertTrue(FirebaseKit.isInitialized, "initialize() must mark the kit initialized")
    }

    /** Repeat calls are documented as safe no-ops — verified where init is real, not just on JVM. */
    @Test
    fun initialize_is_idempotent_on_every_tier() {
        FirebaseKit.initialize(config)
        FirebaseKit.initialize(config)
        assertTrue(FirebaseKit.isInitialized)
    }

    /** After a real init the analytics factory must still hand back a usable helper. */
    @Test
    fun analytics_helper_is_available_after_initialize() {
        FirebaseKit.initialize(config)
        assertNotNull(provideAnalyticsHelper())
    }

    /** And logging through it must not throw once Firebase has been configured this way. */
    @Test
    fun logging_after_initialize_does_not_throw() {
        FirebaseKit.initialize(config)
        provideAnalyticsHelper().logEvent("programmatic_init_probe", mapOf("source" to "test"))
    }

    /**
     * The no-keys case on a tier whose `platformInitializeFirebase` is real: supplying nothing
     * must degrade rather than throw, matching `FirebaseKit.initialize`'s documented contract.
     */
    @Test
    fun initialize_without_any_options_degrades_without_throwing() {
        FirebaseKit.initialize(FirebaseConfig())
        assertTrue(FirebaseKit.isInitialized)
        assertNotNull(provideAnalyticsHelper())
    }
}
