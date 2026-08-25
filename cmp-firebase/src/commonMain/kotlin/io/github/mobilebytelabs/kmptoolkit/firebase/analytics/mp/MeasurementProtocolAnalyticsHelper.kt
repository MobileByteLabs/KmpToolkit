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

import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.EventTypes
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.kmpPlatform
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.withPlatform
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.concurrent.Volatile

private const val TAG = "MpAnalytics"
private const val MAX_EVENTS_PER_REQUEST = 25
private const val DEBOUNCE_MILLIS = 5_000L

/**
 * [AnalyticsHelper] backed by Firebase Measurement Protocol over HTTP.
 *
 * Used on KMP targets where GitLive's Firebase Analytics SDK does not ship
 * (the nonFirebaseMain tier: JVM, Linux ×2, mingwX64, wasmJs). Events sent via this
 * helper land in the **same Firebase Analytics property + same BigQuery
 * export** as events sent via GitLive's [FirebaseAnalyticsHelper] — the
 * `/idea analytics --fetch` flow is transport-agnostic.
 *
 * **What you give up vs native SDK:**
 * - No automatic events (`first_open`, `session_start`, `in_app_purchase`)
 * - No DebugView (events visible only in BigQuery, ~1h latency)
 * - No A/B Testing tie-in
 * - No demographics inference
 * - Persistent `client_id` only on platforms with KV storage (see [ClientId])
 *
 * **What still works:**
 * - Custom event capture with up to 25 params per event
 * - User properties (via [setUserProperty])
 * - User ID (via [setUserId])
 * - Manual screen-view tracking (via [logScreenView])
 * - Persistent client_id where the platform supports KV storage
 *
 * **Configuration**: requires a [MpConfig] with the GA4 measurement ID and
 * an MP API secret. Generate at Firebase Console → Project settings →
 * Integrations → GA4 → Data Streams → {stream} → Measurement Protocol API
 * secrets → Create.
 *
 * **Batching**: events are buffered in-memory and flushed every 5 seconds OR
 * when the buffer hits 25 events (the per-request MP limit), whichever first.
 * On JVM/Apple/Linux this happens on a background coroutine. On wasm the
 * scheduler runs on the same event loop.
 *
 * **Network failure**: silently dropped per the analytics-must-not-break-
 * the-app principle. Failed sends ARE logged via Kermit at WARN level so
 * dev builds can spot misconfiguration during testing.
 */
class MeasurementProtocolAnalyticsHelper(
    private val config: MpConfig,
    settings: Settings,
    httpClient: HttpClient? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    /**
     * Override the auto-injected `kmp_platform` value. Defaults to [kmpPlatform]
     * (`"watchos" | "linux" | "mingw" | "wasmjs" | "wasmwasi"` on this tier).
     */
    platformOverride: String? = null,
) : AnalyticsHelper {

    private val client: HttpClient = httpClient ?: HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private val clientId: String = loadOrCreateClientId(settings)
    private val platform: String = platformOverride ?: kmpPlatform

    @Volatile private var userId: String? = null

    @Volatile private var collectionEnabled: Boolean = true
    private val userProperties = mutableMapOf<String, String>()

    private val buffer = mutableListOf<AnalyticsEvent>()
    private val bufferMutex = Mutex()

    @Volatile private var flushJob: Job? = null

    override fun logEvent(event: AnalyticsEvent) {
        if (!collectionEnabled) return // opt-out: drop the event, never buffer or POST it
        val enriched = event.withPlatform(platform)
        scope.launch {
            bufferMutex.withLock {
                buffer.add(enriched)
                // A crash mirror must not wait out the 5s debounce — a fatal crash exits
                // the process first and the event would be lost. Flush immediately (still
                // async; a truly instant exit can't be beaten without blocking the POST).
                if (buffer.size >= MAX_EVENTS_PER_REQUEST || event.type == EventTypes.APP_CRASH) {
                    flushNowLocked()
                } else {
                    scheduleFlush()
                }
            }
        }
    }

    override fun setUserProperty(name: String, value: String) {
        userProperties[name.take(MAX_PROP_NAME)] = value.take(MAX_PROP_VALUE)
    }

    override fun setUserId(userId: String) {
        this.userId = userId.take(MAX_USER_ID).takeIf { it.isNotBlank() }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        collectionEnabled = enabled
    }

    override fun setConsent(analyticsStorage: Boolean, adStorage: Boolean) {
        // The Measurement-Protocol tier has no native Consent Mode; honour
        // analytics_storage as the collection gate (ad_storage is n/a here).
        collectionEnabled = analyticsStorage
    }

    /**
     * Force a flush now. Useful before app shutdown if you want to send
     * pending events synchronously. Best-effort — ignores network errors.
     */
    suspend fun flush() {
        bufferMutex.withLock { flushNowLocked() }
    }

    /** Releases coroutine scope + HTTP client. Call from app shutdown. */
    fun close() {
        scope.cancel()
        client.close()
    }

    // ── private ─────────────────────────────────────────────────────────────

    private fun scheduleFlush() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(DEBOUNCE_MILLIS)
            bufferMutex.withLock { flushNowLocked() }
        }
    }

    private suspend fun flushNowLocked() {
        if (buffer.isEmpty()) return
        val toSend = buffer.toList()
        buffer.clear()
        flushJob?.cancel()
        flushJob = null
        sendBatch(toSend)
    }

    private suspend fun sendBatch(events: List<AnalyticsEvent>) {
        val payload = buildPayload(events)
        try {
            client.post(config.endpoint) {
                parameter("measurement_id", config.measurementId)
                parameter("api_secret", config.apiSecret)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (t: Throwable) {
            // Never let analytics break the app. Log at WARN so dev builds catch misconfig.
            Logger.w(TAG, t) { "MP send failed (${events.size} events dropped): ${t.message}" }
        }
    }

    private fun buildPayload(events: List<AnalyticsEvent>): MpRequest = MpRequest(
        clientId = clientId,
        userId = userId,
        userProperties = if (userProperties.isEmpty()) {
            null
        } else {
            userProperties.mapValues {
                UserPropertyValue(it.value.take(MAX_PROP_VALUE))
            }
        },
        events = events.map { evt ->
            MpEvent(
                name = evt.type.take(MAX_EVENT_NAME),
                params = if (evt.extras.isEmpty()) {
                    null
                } else {
                    evt.extras.associate {
                        it.key.take(MAX_PARAM_KEY) to JsonPrimitive(it.value.take(MAX_PARAM_VALUE))
                    }
                },
            )
        },
    )

    private companion object {
        const val MAX_EVENT_NAME = 40
        const val MAX_PARAM_KEY = 40
        const val MAX_PARAM_VALUE = 100
        const val MAX_PROP_NAME = 24
        const val MAX_PROP_VALUE = 36
        const val MAX_USER_ID = 256
    }
}

// ── MP request payload (kotlinx.serialization) ──────────────────────────────
// Schema: https://developers.google.com/analytics/devguides/collection/protocol/ga4

// `internal` (not `private`) purely so MpSerializationTest can lock the snake_case
// contract in-module; NOT part of the public API (absent from the BCV .api dump).
@Serializable
internal data class MpRequest(
    // GA4 Measurement Protocol requires snake_case keys; without these @SerialName
    // overrides the payload ships `clientId`/`userId`/`userProperties`, GA4 returns
    // 2xx but SILENTLY DROPS the event (no valid `client_id`). This defeats the entire
    // MP tier (all non-GitLive analytics + the fallback-tier crash→GA4 mirror).
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_properties") val userProperties: Map<String, UserPropertyValue>? = null,
    val events: List<MpEvent>,
)

@Serializable
internal data class UserPropertyValue(val value: String)

@Serializable
internal data class MpEvent(val name: String, val params: Map<String, kotlinx.serialization.json.JsonElement>? = null)
