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

import co.touchlab.kermit.Logger

private const val TAG = "StubAnalytics"

/**
 * Development implementation of [AnalyticsHelper] that logs events to Kermit.
 *
 * Used for:
 * - Debug builds where you want visibility into event firing
 * - Local testing during instrumentation development
 * - Offline development without analytics backend connectivity
 *
 * Auto-injects the `kmp_platform` param on every event so dev logs match what
 * production [io.github.mobilebytelabs.kmptoolkit.firebase.analytics.FirebaseAnalyticsHelper]
 * and [io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MeasurementProtocolAnalyticsHelper]
 * actually capture — same shape in BigQuery as in your debug Logcat.
 *
 * Output format:
 * ```
 * I StubAnalytics: 📊 button_click  {kmp_platform=android, button_name=save, screen_name=settings}
 * I StubAnalytics: 👤 setUserProperty  user_type = premium
 * I StubAnalytics: 🆔 setUserId  abc123
 * ```
 */
class StubAnalyticsHelper(
    /**
     * Override the auto-injected `kmp_platform` value. Defaults to [kmpPlatform].
     */
    platformOverride: String? = null,
) : AnalyticsHelper {

    private val platform: String = platformOverride ?: kmpPlatform

    override fun logEvent(event: AnalyticsEvent) {
        val enriched = event.withPlatform(platform)
        val params = if (enriched.extras.isEmpty()) {
            ""
        } else {
            enriched.extras.joinToString(", ", prefix = "{", postfix = "}") { "${it.key}=${it.value}" }
        }
        Logger.i(TAG) { "📊 ${enriched.type}  $params" }
    }

    override fun setUserProperty(name: String, value: String) {
        Logger.i(TAG) { "👤 setUserProperty  $name = $value" }
    }

    override fun setUserId(userId: String) {
        Logger.i(TAG) { "🆔 setUserId  $userId" }
    }
}
