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

import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.CrashReporter
import io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics.provideCrashReporter

/**
 * Single, in-library setup surface for Firebase across every KMP target —
 * **"setup stays in the library"**.
 *
 * [initialize] enables crash reporting and wires the platform [crashReporter]
 * in one idempotent call. What each platform needs:
 *
 * - **Android** — nothing to call. A `ContentProvider` (`FirebaseInitProvider`)
 *   runs [initialize] automatically at process start; Firebase auto-reads
 *   `google-services.json`. Zero app code.
 * - **iOS / macOS / tvOS** — call [initialize] once from your entry point. Native
 *   Firebase Crashlytics then auto-captures uncaught crashes.
 * - **JVM / JS / watchOS / Linux / Windows / wasmJs** — [initialize] activates the
 *   structured logging [crashReporter]; route your exception handler to it.
 *
 * ### The only residual app-side steps (Firebase constraints, not library gaps)
 * These identify YOUR Firebase project, so Firebase itself requires them in the app:
 * - **Android**: add `google-services.json` + apply `com.google.gms.google-services`.
 * - **Apple**: add `GoogleService-Info.plist` and the one Swift line
 *   `FirebaseApp.configure()` in your `@main` `init` (GitLive's documented path).
 * - **JS**: pass your web config via `Firebase.initialize(options = FirebaseOptions(...))`.
 *
 * ### Usage
 * ```kotlin
 * // Non-Android entry point (Apple @main, desktop main(), JS bootstrap):
 * FirebaseKit.initialize()
 *
 * // Anywhere after init — rich, AI-feedable capture:
 * try { risky() } catch (t: Throwable) {
 *     FirebaseKit.crashReporter.recordException(t)
 * }
 * ```
 */
object FirebaseKit {

    private var initialized = false

    /**
     * The platform default [CrashReporter] — native Firebase Crashlytics on the 9
     * targets GitLive supports, a structured logging reporter everywhere else.
     * Memoized; safe to read before [initialize].
     */
    val crashReporter: CrashReporter by lazy { provideCrashReporter() }

    /** `true` once [initialize] has run in this process. */
    val isInitialized: Boolean get() = initialized

    /**
     * Enable crash reporting and install what the platform allows. Idempotent —
     * safe to call from the Android auto-init provider and from app code both.
     */
    fun initialize() {
        if (initialized) return
        crashReporter.install()
        initialized = true
    }
}
