/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics.net

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.Param
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request

/** Config for [analyticsTelemetryPlugin]. */
class AnalyticsTelemetryConfig {
    /** The helper events are logged to. MUST be set. */
    var analytics: AnalyticsHelper? = null

    /** Gate — return false to suppress (e.g. when collection is disabled). */
    var enabled: () -> Boolean = { true }
}

/**
 * A Ktor client plugin that logs a `http_request` analytics event per response with
 * endpoint (host + path), HTTP status, status class (2xx/4xx/5xx), latency in ms, and a
 * coarse latency bucket — the per-request signal you trend release-over-release, per platform.
 *
 * ```kotlin
 * val client = HttpClient { install(analyticsTelemetryPlugin(analytics)) }
 * ```
 */
fun analyticsTelemetryPlugin(helper: AnalyticsHelper, enabled: () -> Boolean = { true }) =
    createClientPlugin("AnalyticsTelemetry", ::AnalyticsTelemetryConfig) {
        pluginConfig.analytics = helper
        pluginConfig.enabled = enabled
        val analytics = pluginConfig.analytics
        val isEnabled = pluginConfig.enabled

        onResponse { response ->
            if (analytics == null || !isEnabled()) return@onResponse
            val latencyMs = (response.responseTime.timestamp - response.requestTime.timestamp)
                .coerceAtLeast(0)
            val url = response.request.url
            val status = response.status.value
            analytics.logEvent(
                AnalyticsEvent(
                    EVENT_HTTP,
                    listOf(
                        Param(PARAM_ENDPOINT, "${url.host}${url.encodedPath}"),
                        Param(PARAM_STATUS, status.toString()),
                        Param(PARAM_STATUS_CLASS, "${status / 100}xx"),
                        Param(PARAM_LATENCY, latencyMs.toString()),
                        Param(PARAM_LATENCY_BUCKET, latencyBucket(latencyMs)),
                    ),
                ),
            )
        }
    }

private const val EVENT_HTTP = "http_request"
private const val PARAM_ENDPOINT = "endpoint"
private const val PARAM_STATUS = "status"
private const val PARAM_STATUS_CLASS = "status_class"
private const val PARAM_LATENCY = "latency_ms"
private const val PARAM_LATENCY_BUCKET = "latency_bucket"

private fun latencyBucket(ms: Long): String = when {
    ms < 100 -> "<100ms"
    ms < 300 -> "100-300ms"
    ms < 1000 -> "300ms-1s"
    ms < 3000 -> "1-3s"
    else -> ">3s"
}
