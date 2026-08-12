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
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.FirebaseCrashlytics
import dev.gitlive.firebase.crashlytics.crashlytics
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.kmpPlatform

private const val TAG = "CmpCrash"
private const val MAX_BREADCRUMBS = 64

/**
 * The native-tier [CrashReporter] for the 6 targets GitLive Crashlytics ships on
 * (Android, iOS ×3, macOS ×2).
 *
 * Forwards to native Firebase Crashlytics — which auto-captures uncaught crashes
 * on-device — AND builds the same structured [CrashReport] as the fallback tier,
 * so [lastReport] is available for AI hand-off on every platform uniformly.
 *
 * @param crashlyticsProvider indirection so tests can inject a fake; defaults to
 *   the GitLive `Firebase.crashlytics` singleton, resolved lazily so it is not
 *   touched before `FirebaseApp` is configured.
 */
class FirebaseCrashReporter(
    crashlyticsProvider: () -> FirebaseCrashlytics = { Firebase.crashlytics },
    private val platform: String = kmpPlatform,
) : CrashReporter {

    private val crashlytics: FirebaseCrashlytics by lazy(crashlyticsProvider)

    private val stickyKeys = mutableMapOf<String, String>()
    private val breadcrumbs = ArrayDeque<String>()
    private var userId: String? = null
    private var _lastReport: CrashReport? = null

    override val lastReport: CrashReport? get() = _lastReport

    override fun recordException(throwable: Throwable, fatal: Boolean, extraKeys: Map<String, String>) {
        extraKeys.forEach { (k, v) -> runCatching { crashlytics.setCustomKey(k, v) } }
        runCatching { crashlytics.recordException(throwable) }
            .onFailure { Logger.w(TAG) { "native recordException failed: ${it.message}" } }

        val keys = buildMap {
            putAll(stickyKeys)
            putAll(extraKeys)
            userId?.let { put("user_id", it) }
        }
        _lastReport = throwable.toCrashReport(
            platform = platform,
            fatal = fatal,
            customKeys = keys,
            breadcrumbs = breadcrumbs.toList(),
        )
    }

    override fun log(message: String) {
        breadcrumbs.addLast(message)
        while (breadcrumbs.size > MAX_BREADCRUMBS) breadcrumbs.removeFirst()
        runCatching { crashlytics.log(message) }
    }

    override fun setCustomKey(key: String, value: String) {
        stickyKeys[key] = value
        runCatching { crashlytics.setCustomKey(key, value) }
    }

    override fun setUserId(userId: String) {
        this.userId = userId
        runCatching { crashlytics.setUserId(userId) }
    }

    override fun install() {
        runCatching { crashlytics.setCrashlyticsCollectionEnabled(true) }
            .onFailure { Logger.w(TAG) { "could not enable Crashlytics collection: ${it.message}" } }
    }
}
