/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

/**
 * Platform-agnostic crash + non-fatal reporting.
 *
 * Two implementations ship:
 * - **[io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics] FirebaseCrashReporter**
 *   (Android, iOS ×3, macOS ×2 — the 6 targets GitLive Crashlytics supports):
 *   forwards to native Firebase Crashlytics AND builds a structured [CrashReport].
 * - **[LoggingCrashReporter]** (JVM, JS, tvOS ×3, watchOS ×4, Linux ×2, mingw, wasmJs
 *   — the 13 targets Crashlytics does not reach): builds the same [CrashReport] and
 *   logs it as JSON via Kermit.
 *
 * Either way [lastReport] holds the most recent structured report, ready to hand
 * to an AI. Obtain the platform default via
 * [io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit.crashReporter].
 */
interface CrashReporter {

    /**
     * Record a caught exception. On native-Crashlytics targets this is a
     * non-fatal record (or fatal if [fatal] is true); everywhere it (re)builds
     * [lastReport].
     *
     * @param extraKeys one-off keys merged over the sticky [setCustomKey] keys for this record
     */
    fun recordException(
        throwable: Throwable,
        fatal: Boolean = false,
        extraKeys: Map<String, String> = emptyMap(),
    )

    /** Add a breadcrumb log line (bounded ring buffer, surfaced in [CrashReport.breadcrumbs]). */
    fun log(message: String)

    /** Set a sticky custom key attached to every subsequent report. */
    fun setCustomKey(key: String, value: String)

    /** Associate crashes with an obfuscated user id. NEVER pass PII. */
    fun setUserId(userId: String)

    /**
     * Enable collection and install whatever automatic capture the platform
     * allows. Idempotent. Called by
     * [io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit.initialize].
     */
    fun install()

    /** The most recent structured report, or `null` before the first record. */
    val lastReport: CrashReport?
}

/**
 * Platform factory for the default [CrashReporter]:
 * - native-Crashlytics tier → `FirebaseCrashReporter(Firebase.crashlytics)`
 * - fallback tier → [LoggingCrashReporter]
 *
 * Prefer [io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit.crashReporter],
 * which memoizes the result of this factory.
 */
expect fun provideCrashReporter(): CrashReporter
