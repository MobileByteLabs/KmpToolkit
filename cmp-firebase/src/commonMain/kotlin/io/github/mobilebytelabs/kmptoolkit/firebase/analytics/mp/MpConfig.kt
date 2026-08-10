/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp

/**
 * Firebase Measurement Protocol configuration.
 *
 * Required for [MeasurementProtocolAnalyticsHelper]. Generate API secrets at:
 * Firebase Console → Project settings → Integrations → GA4 → Data Streams →
 * {your stream} → **Measurement Protocol API secrets** → Create.
 *
 * @property measurementId GA4 measurement ID — looks like `"G-XXXXXXXX"`.
 *   Same value as `analytics.envs.{env}.property_id` in your project's
 *   `idea-layer/PROJECT_CONFIG.yaml`.
 *
 * @property apiSecret Measurement Protocol API secret. Sensitive — load from
 *   app secrets store (env var, encrypted prefs, keychain), NEVER hard-code.
 *
 * @property endpoint The MP collection endpoint. Defaults to GA4 production.
 *   Override only for testing (e.g., MP debug endpoint
 *   `https://www.google-analytics.com/debug/mp/collect`).
 */
data class MpConfig(val measurementId: String, val apiSecret: String, val endpoint: String = DEFAULT_ENDPOINT) {
    init {
        require(measurementId.isNotBlank()) { "MpConfig.measurementId cannot be blank" }
        require(measurementId.startsWith("G-")) {
            "MpConfig.measurementId must be a GA4 ID starting with 'G-' (got: '$measurementId')"
        }
        require(apiSecret.isNotBlank()) { "MpConfig.apiSecret cannot be blank" }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://www.google-analytics.com/mp/collect"
        const val DEBUG_ENDPOINT = "https://www.google-analytics.com/debug/mp/collect"
    }
}
