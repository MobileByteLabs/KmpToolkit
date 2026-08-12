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

import co.touchlab.kermit.Logger
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.kmpPlatform

private const val TAG = "CmpCrash"
private const val MAX_BREADCRUMBS = 64

/**
 * The fallback [CrashReporter] for the 13 targets GitLive Crashlytics doesn't
 * ship on (JVM, JS, tvOS, watchOS, Linux, mingw, wasmJs).
 *
 * There is no Crashlytics ingestion REST API, so instead of silently dropping
 * the signal this builds the same rich [CrashReport] and **logs it as pretty
 * JSON via Kermit** — visible in your desktop/web console and ready to copy into
 * an AI for diagnosis. Sticky keys, a breadcrumb ring buffer, and the obfuscated
 * user id are all folded into the report.
 *
 * Automatic uncaught-exception capture is platform-specific and not wired here;
 * route your global handler (or a [asCoroutineExceptionHandler]) to
 * [recordException] to capture crashes on these targets.
 */
class LoggingCrashReporter(private val platform: String = kmpPlatform) : CrashReporter {

    private val stickyKeys = mutableMapOf<String, String>()
    private val breadcrumbs = ArrayDeque<String>()
    private var userId: String? = null
    private var _lastReport: CrashReport? = null

    override val lastReport: CrashReport? get() = _lastReport

    override fun recordException(throwable: Throwable, fatal: Boolean, extraKeys: Map<String, String>) {
        val keys = buildMap {
            putAll(stickyKeys)
            putAll(extraKeys)
            userId?.let { put("user_id", it) }
        }
        val report = throwable.toCrashReport(
            platform = platform,
            fatal = fatal,
            customKeys = keys,
            breadcrumbs = breadcrumbs.toList(),
        )
        _lastReport = report
        Logger.e(TAG) { "🔥 crash captured (fatal=$fatal)\n${report.toJson(pretty = true)}" }
    }

    override fun log(message: String) {
        breadcrumbs.addLast(message)
        while (breadcrumbs.size > MAX_BREADCRUMBS) breadcrumbs.removeFirst()
        Logger.i(TAG) { "📝 $message" }
    }

    override fun setCustomKey(key: String, value: String) {
        stickyKeys[key] = value
    }

    override fun setUserId(userId: String) {
        this.userId = userId
    }

    override fun install() {
        Logger.i(TAG) {
            "LoggingCrashReporter active on '$platform'. GitLive Crashlytics is unavailable here — " +
                "route your global/coroutine exception handler to recordException() to capture crashes."
        }
    }
}
