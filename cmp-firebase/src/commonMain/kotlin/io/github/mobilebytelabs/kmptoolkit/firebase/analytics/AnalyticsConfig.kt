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
 * Startup configuration for an [AnalyticsHelper], applied by
 * [io.github.mobilebytelabs.kmptoolkit.firebase.analytics.di.AnalyticsModule.analyticsHelper]
 * at creation time so consumers can inject once and just call `logEvent(...)`.
 *
 * ```kotlin
 * // opt-in-by-default (collection on immediately)
 * val analytics = AnalyticsModule.analyticsHelper(Mode.Firebase)
 *
 * // opt-in-required (GDPR): start OFF, enable after the user consents
 * val analytics = AnalyticsModule.analyticsHelper(
 *     Mode.Firebase,
 *     AnalyticsConfig(collectionEnabledByDefault = false),
 * )
 * // later, on consent:
 * analytics.setCollectionEnabled(true)
 * ```
 */
data class AnalyticsConfig(
    /**
     * Whether analytics collection — including Firebase's automatic user-acquisition and
     * behaviour/engagement events — is enabled at startup.
     *
     * `true` (default) = opt-in-by-default: collection begins immediately.
     * `false` = opt-in-required: nothing is collected until [AnalyticsHelper.setCollectionEnabled]`(true)`.
     */
    val collectionEnabledByDefault: Boolean = true,
    /** Auto-log `screen_view` (+ `screen_transition`) from the Compose companion. */
    val autoScreenTracking: Boolean = true,
    /** Auto-log `app_launch` cold-start timing via `AppLifecycleTracker`. */
    val autoAppLaunchTiming: Boolean = true,
    /** Accumulate P95/P99 performance stats via `PerformanceTracker`. */
    val autoPerformanceStats: Boolean = true,
    /** Auto-log network transitions + per-request Ktor telemetry. */
    val autoNetworkTelemetry: Boolean = true,
    /** Route uncaught failures to the `CrashReporter`. */
    val autoCrashCapture: Boolean = true,
    /** Buffer events while offline and flush on reconnect via `OfflineEventQueue`. */
    val autoOfflineQueue: Boolean = true,
    /** Durations at/above this are tagged `slow` by `PerformanceTracker`. */
    val slowOperationThresholdMs: Long = 1000L,
) {
    /**
     * Every auto-tracker checks this before emitting, so the master
     * [AnalyticsHelper.setCollectionEnabled]`(false)` opt-out (and each per-capability flag)
     * suppresses the corresponding automatic events.
     */
    fun autoEnabled(capability: Boolean, collectionEnabled: Boolean): Boolean = collectionEnabled && capability
}
