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
 * - **[LoggingCrashReporter]** (JVM, JS, tvOS ×3, Linux ×2, mingw, wasmJs
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
    fun recordException(throwable: Throwable, fatal: Boolean = false, extraKeys: Map<String, String> = emptyMap())

    /** Add a breadcrumb log line (bounded ring buffer, surfaced in [CrashReport.breadcrumbs]). */
    fun log(message: String)

    /** Set a sticky custom key attached to every subsequent report. */
    fun setCustomKey(key: String, value: String)

    /** Associate crashes with an obfuscated user id. NEVER pass PII. */
    fun setUserId(userId: String)

    /**
     * Install whatever automatic capture the platform allows. Idempotent. Called by
     * [io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit.initialize].
     *
     * It does NOT switch collection on: the app's declared platform default stands (Android
     * `firebase_crashlytics_collection_enabled` meta-data, Apple `FirebaseCrashlyticsCollectionEnabled`),
     * and only [setCollectionEnabled] changes it. An app that gates crash reporting on consent keeps it
     * off from the very first launch.
     */
    fun install()

    /**
     * Give or withdraw permission to send crash data off the device — the consent switch (GDPR Art. 7).
     *
     * `false` stops forwarding to every remote sink (native Crashlytics and the GA4 `app_crash` mirror) and
     * discards reports the SDK cached but has not sent; [lastReport] is still built locally. `true`
     * resumes forwarding. Persisted by the native SDK, so call it with the user's stored choice on every
     * start. Default: no-op, for reporters that send nothing anywhere.
     */
    fun setCollectionEnabled(enabled: Boolean) {}

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
