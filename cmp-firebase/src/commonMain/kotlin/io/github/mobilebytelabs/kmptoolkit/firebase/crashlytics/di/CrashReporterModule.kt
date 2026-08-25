/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.di

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.provideAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.CrashReporter
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.LoggingCrashReporter
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.NoOpCrashReporter
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.provideCrashReporter

/**
 * Factory for a [CrashReporter] given a build-time mode — mirrors
 * `AnalyticsModule` and, like it, intentionally defines NO Koin `Module` so it
 * forces no Koin dependency on consumers.
 *
 * ```kotlin
 * val firebaseModule = module {
 *     single<CrashReporter> {
 *         CrashReporterModule.crashReporter(
 *             if (BuildConfig.DEBUG) CrashReporterModule.Mode.Logging
 *             else                   CrashReporterModule.Mode.Firebase
 *         ).also { it.install() }
 *     }
 * }
 * ```
 */
object CrashReporterModule {

    /** Build mode selector. */
    enum class Mode {
        /** Production: native Firebase Crashlytics where supported, structured logging elsewhere. */
        Firebase,

        /** Development: always the structured [LoggingCrashReporter] (JSON to Kermit). */
        Logging,

        /** Tests / previews: silently discard. */
        NoOp,
    }

    fun crashReporter(mode: Mode = Mode.Firebase): CrashReporter = when (mode) {
        Mode.Firebase -> provideCrashReporter()
        // Mirror to the same all-platform GA4 crash view as Mode.Firebase — without the
        // sink, the documented DEBUG wiring silently disabled the app_crash bridge.
        Mode.Logging -> LoggingCrashReporter(analyticsSink = { provideAnalyticsHelper() })
        Mode.NoOp -> NoOpCrashReporter
    }
}
