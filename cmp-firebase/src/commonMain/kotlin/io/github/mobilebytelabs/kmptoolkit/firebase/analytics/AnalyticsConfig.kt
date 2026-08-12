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
)
