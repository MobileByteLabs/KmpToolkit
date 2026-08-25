/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.NoOpAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.PerformanceTracker
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.StubAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.provideAnalyticsHelper

/**
 * Factory for an [AnalyticsHelper] given a build-time mode.
 *
 * The recommended pattern is `Mode.Firebase` (production) and `Mode.Stub` (debug):
 *
 * ```kotlin
 * val analyticsModule = module {
 *     single<AnalyticsHelper> {
 *         AnalyticsModule.analyticsHelper(
 *             if (BuildConfig.DEBUG) AnalyticsModule.Mode.Stub
 *             else                   AnalyticsModule.Mode.Firebase
 *         )
 *     }
 *     single { AnalyticsModule.performanceTracker(get()) }
 * }
 * ```
 *
 * `Mode.Firebase` calls [provideAnalyticsHelper], which returns (matching build.gradle.kts):
 * - On Android/iOS/macOS/tvOS/JS (firebaseMain): `FirebaseAnalyticsHelper(Firebase.analytics)`
 * - On JVM/Linux/Windows/wasm (nonFirebaseMain): Measurement-Protocol helper when an
 *   `MpConfig` is configured via `FirebaseKit.initialize(config)`, else [NoOpAnalyticsHelper]
 *
 * Note: this module intentionally does NOT define a Koin `Module` to avoid forcing a
 * Koin dependency on every consumer.
 */
object AnalyticsModule {

    /** Build mode selector. */
    enum class Mode {
        /** Production: Firebase Analytics where supported, NoOp elsewhere. */
        Firebase,

        /** Development: log events to Kermit (visible in IDE/console). */
        Stub,

        /** Tests / Compose previews: silently discard events. */
        NoOp,
    }

    /**
     * Create the [AnalyticsHelper] for [mode] and apply [config] at startup — notably
     * [AnalyticsConfig.collectionEnabledByDefault], so the returned helper is already in the
     * right opt-in/opt-out state. Inject the result once and just call `logEvent(...)`.
     */
    fun analyticsHelper(mode: Mode = Mode.NoOp, config: AnalyticsConfig = AnalyticsConfig()): AnalyticsHelper =
        when (mode) {
            Mode.Firebase -> provideAnalyticsHelper()
            Mode.Stub -> StubAnalyticsHelper()
            Mode.NoOp -> NoOpAnalyticsHelper
        }.also { it.setCollectionEnabled(config.collectionEnabledByDefault) }

    fun performanceTracker(helper: AnalyticsHelper): PerformanceTracker = PerformanceTracker(helper)
}
