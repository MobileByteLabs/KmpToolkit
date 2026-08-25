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

/**
 * Per-platform default-helper factory (the raw, un-memoized construction). Internal —
 * consumers call [provideAnalyticsHelper], which memoizes the result process-wide.
 *
 * Returns a platform-appropriate [AnalyticsHelper], matching the build.gradle.kts
 * source-set wiring (firebaseMain vs nonFirebaseMain) exactly:
 *
 * | Tier | Targets | Returns |
 * |---|---|---|
 * | **firebaseMain** (GitLive) | Android, iOS (×3), macOS (×2), tvOS (×3), JS | `FirebaseAnalyticsHelper(Firebase.analytics)` |
 * | **nonFirebaseMain** | JVM, Linux (×2), mingwX64, wasmJs | [MeasurementProtocolAnalyticsHelper] when an [io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig] is configured, else [NoOpAnalyticsHelper] |
 *
 * Note: JVM/desktop is on the **nonFirebaseMain** tier — GitLive Firebase Analytics
 * does NOT ship for JVM. Desktop analytics (and the desktop crash→GA4 mirror) requires
 * `FirebaseKit.initialize(config)` with an `MpConfig`; otherwise it NoOps.
 */
internal expect fun createPlatformAnalyticsHelper(): AnalyticsHelper

/**
 * The process-wide default [AnalyticsHelper] — memoized so the app's DI and the
 * crash→GA4 bridge share ONE instance (authoritative consent + stable MP `client_id`).
 *
 * For finer control (debug → Stub, release → Firebase), construct your own helper in DI:
 * ```kotlin
 * single<AnalyticsHelper> { if (BuildConfig.DEBUG) StubAnalyticsHelper() else provideAnalyticsHelper() }
 * ```
 */
fun provideAnalyticsHelper(): AnalyticsHelper =
    io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime.analyticsHelper
        ?: createPlatformAnalyticsHelper().also {
            io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime.analyticsHelper = it
        }
