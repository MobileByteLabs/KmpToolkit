/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe.hooks

import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * T2/T4 hook: POSTs structured library events to framework-supabase.library_events.
 *
 * Authentication: per-consumer anon-key (Bearer JWT) issued via /secrets push
 * (Phase 03 T7 — library-events-anon-key-{consumer} secret).
 *
 * Batching: queues events in memory; flushes on:
 *   - queue.size >= 50
 *   - explicit [flush] call (e.g. at app close)
 * Failure: re-enqueues batch for retry on next flush; never blocks calling thread.
 *
 * Signal tier: caller chooses T2 (lifecycle) or T4 (full API usage) at construction.
 *
 * Per GOAL.md AC #16: POST with valid consumer anon-key → row in library_events;
 * RLS denies POST with revoked/unknown anon-key.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T7.
 */
@OptIn(ExperimentalAtomicApi::class)
public class SupabaseEventsHook(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val consumerAppId: String,
    private val httpClient: HttpClient,
    private val coroutineScope: CoroutineScope,
    private val signalTier: String = "T2",
    private val anonymizedUserIdProvider: (() -> String?)? = null,
) : LibraryObservationHook {

    init {
        require(signalTier == "T2" || signalTier == "T4") {
            "signalTier must be 'T2' or 'T4' (got: $signalTier)"
        }
    }

    // AtomicReference + CAS retry loop — multiplatform-safe replacement for
    // `synchronized(queueLock)`. Compiles on all 10 KMP targets cmp-observe ships.
    private val queueRef: AtomicReference<List<LibraryEvent>> = AtomicReference(emptyList())
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Serializable
    private data class LibraryEvent(
        val consumer_app_id: String,
        val library_name: String,
        val library_version: String,
        val event_type: String,
        val signal_tier: String,
        val payload: String,
        val anonymized_user_id: String? = null,
    )

    override fun onInitStart(meta: CmpMetadata) {
        // No-op — emit only on complete/failure/lifecycle/close to keep volume bounded.
    }

    override fun onInitComplete(meta: CmpMetadata) {
        enqueue(meta, "init_complete", emptyMap())
    }

    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {
        enqueue(meta, "init_failure", mapOf("reason" to (throwable.message ?: "unknown")))
    }

    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
        enqueue(meta, event, payload)
    }

    override fun onClose(meta: CmpMetadata) {
        enqueue(meta, "close", emptyMap())
        // Best-effort flush on close.
        coroutineScope.launch { flush() }
    }

    private fun enqueue(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
        val sanitized = payload.mapValues { it.value?.toString() ?: "null" }
        val ev = LibraryEvent(
            consumer_app_id = consumerAppId,
            library_name = meta.name,
            library_version = meta.version,
            event_type = event,
            signal_tier = signalTier,
            payload = json.encodeToString(kotlinx.serialization.serializer<Map<String, String>>(), sanitized),
            anonymized_user_id = anonymizedUserIdProvider?.invoke(),
        )
        val updated = updateQueue { it + ev }
        if (updated.size >= 50) coroutineScope.launch { flush() }
    }

    /**
     * Flush the current queue to framework-supabase. Safe to call concurrently;
     * a single in-flight batch is retried on failure (re-enqueued at the head).
     */
    public suspend fun flush() {
        // Atomically drain the queue.
        val batch = drainQueue()
        if (batch.isEmpty()) return
        try {
            httpClient.post("$supabaseUrl/rest/v1/library_events") {
                header(HttpHeaders.Authorization, "Bearer $anonKey")
                header("apikey", anonKey)
                header("Prefer", "return=minimal")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(ListSerializer(LibraryEvent.serializer()), batch))
            }
        } catch (_: Throwable) {
            // Re-enqueue at head on failure for retry.
            updateQueue { batch + it }
        }
    }

    private inline fun updateQueue(transform: (List<LibraryEvent>) -> List<LibraryEvent>): List<LibraryEvent> {
        while (true) {
            val current = queueRef.load()
            val next = transform(current)
            if (queueRef.compareAndSet(current, next)) return next
        }
    }

    private fun drainQueue(): List<LibraryEvent> {
        while (true) {
            val current = queueRef.load()
            if (current.isEmpty()) return emptyList()
            if (queueRef.compareAndSet(current, emptyList())) return current
        }
    }
}
