/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.analytics.firebase

import dev.gitlive.firebase.analytics.FirebaseAnalytics
import dev.gitlive.firebase.analytics.logEvent
import io.github.mobilebytelabs.kmptoolkit.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.analytics.kmpPlatform
import io.github.mobilebytelabs.kmptoolkit.analytics.withPlatform

/**
 * Production [AnalyticsHelper] that sends events to Firebase Analytics via GitLive's
 * Firebase Kotlin SDK.
 *
 * **Targets**: Android, iOS, JS, JVM (anywhere GitLive Firebase Analytics ships).
 *
 * **Firebase Analytics constraints** (enforced at this layer to align with backend):
 * - Event name: ≤ 40 characters (truncated)
 * - Param key: ≤ 40 characters (truncated)
 * - Param value: ≤ 100 characters (truncated)
 * - User property name: ≤ 24 characters (truncated)
 * - User property value: ≤ 36 characters (truncated)
 * - Maximum 25 params per event (validated upstream by [AnalyticsEvent.init])
 *
 * **Setup requirements**:
 * - Firebase project with Analytics enabled
 * - Platform-specific Firebase init:
 *   - Android: `google-services.json` + `com.google.gms.google-services` plugin
 *   - iOS: `GoogleService-Info.plist` + `FirebaseApp.configure()` in AppDelegate
 *   - JS: Firebase config object in app init
 *   - JVM: typically not used in production; falls back to NoOp on missing init
 *
 * @sample
 * ```kotlin
 * // Koin wiring — usually in your app's AnalyticsModule.kt
 * import dev.gitlive.firebase.Firebase
 * import dev.gitlive.firebase.analytics.analytics
 *
 * val analyticsModule = module {
 *     single<AnalyticsHelper> {
 *         if (BuildConfig.DEBUG) {
 *             StubAnalyticsHelper()
 *         } else {
 *             FirebaseAnalyticsHelper(Firebase.analytics)
 *         }
 *     }
 * }
 * ```
 */
class FirebaseAnalyticsHelper(
    private val firebase: FirebaseAnalytics,
    /**
     * Override the auto-injected `kmp_platform` value. Defaults to [kmpPlatform]
     * (`"android" | "ios" | "macos" | "tvos" | "jvm" | "js"`).
     *
     * Use to differentiate finer-grained signals like `"android-tv"` or
     * `"jvm-desktop"` — the helper still injects the value under the key
     * `kmp_platform` on every event.
     */
    platformOverride: String? = null,
) : AnalyticsHelper {

    private val platform: String = platformOverride ?: kmpPlatform

    override fun logEvent(event: AnalyticsEvent) {
        val enriched = event.withPlatform(platform)
        firebase.logEvent(enriched.type.take(MAX_EVENT_NAME_LEN)) {
            for (extra in enriched.extras) {
                param(
                    key = extra.key.take(MAX_PARAM_KEY_LEN),
                    value = extra.value.take(MAX_PARAM_VALUE_LEN),
                )
            }
        }
    }

    override fun setUserProperty(name: String, value: String) {
        firebase.setUserProperty(
            name = name.take(MAX_USER_PROP_NAME_LEN),
            value = value.take(MAX_USER_PROP_VALUE_LEN),
        )
    }

    override fun setUserId(userId: String) {
        firebase.setUserId(userId.take(MAX_USER_ID_LEN))
    }

    private companion object {
        const val MAX_EVENT_NAME_LEN = 40
        const val MAX_PARAM_KEY_LEN = 40
        const val MAX_PARAM_VALUE_LEN = 100
        const val MAX_USER_PROP_NAME_LEN = 24
        const val MAX_USER_PROP_VALUE_LEN = 36
        const val MAX_USER_ID_LEN = 256
    }
}
