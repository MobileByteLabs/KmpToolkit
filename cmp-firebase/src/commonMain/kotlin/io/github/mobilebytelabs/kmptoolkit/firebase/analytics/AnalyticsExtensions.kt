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

import kotlin.time.Clock

/**
 * Compose-friendly DSL for building events.
 *
 * @sample
 * ```kotlin
 * analytics.log(EventTypes.FORM_COMPLETED) {
 *     param(ParamKeys.FORM_NAME, "registration")
 *     param(ParamKeys.COMPLETION_TIME, "45s")
 * }
 * ```
 */
inline fun AnalyticsHelper.log(type: String, builder: AnalyticsEventBuilder.() -> Unit = {}) {
    val b = AnalyticsEventBuilder(type)
    b.builder()
    logEvent(b.build())
}

class AnalyticsEventBuilder(private val type: String) {
    private val params = mutableListOf<Param>()

    /** Add a string param. Silently skips invalid keys/values rather than throwing. */
    fun param(key: String, value: String) {
        createParam(key, value)?.let(params::add)
    }

    /** Add a numeric param (converted to String). */
    fun param(key: String, value: Number) = param(key, value.toString())

    /** Add a boolean param. */
    fun param(key: String, value: Boolean) = param(key, value.toString())

    fun build(): AnalyticsEvent = AnalyticsEvent(type, params.toList())
}

// ── Timing ──────────────────────────────────────────────────────────────────

/**
 * A manually-controlled timer. Call [complete] to emit a [EventTypes.LOADING_TIME] event
 * carrying the elapsed duration plus any base params supplied at [startTiming].
 */
class TimedEvent internal constructor(
    private val analytics: AnalyticsHelper,
    private val eventType: String,
    private val baseParams: List<Param>,
) {
    private val startedAt: Long = Clock.System.now().toEpochMilliseconds()

    fun complete(vararg extra: Pair<String, String>) {
        val durationMs = Clock.System.now().toEpochMilliseconds() - startedAt
        val params = buildList {
            addAll(baseParams)
            add(Param(ParamKeys.LOADING_TIME_MS, durationMs.toString()))
            extra.forEach { (k, v) -> createParam(k, v)?.let(::add) }
        }
        analytics.logEvent(AnalyticsEvent(eventType, params))
    }
}

/** Begin a manual [TimedEvent]; call [TimedEvent.complete] when the operation finishes. */
fun AnalyticsHelper.startTiming(eventType: String, vararg params: Pair<String, String>): TimedEvent =
    TimedEvent(this, eventType, params.mapNotNull { createParam(it.first, it.second) })

// ── Batch ───────────────────────────────────────────────────────────────────

/**
 * Collect several events and emit them together on [flush] — handy for a burst of related
 * events you want to log atomically without threading the helper through every call site.
 */
class AnalyticsBatch internal constructor(private val analytics: AnalyticsHelper) {
    private val pending = mutableListOf<AnalyticsEvent>()

    fun add(event: AnalyticsEvent): AnalyticsBatch = apply { pending.add(event) }

    fun add(type: String, vararg params: Pair<String, String>): AnalyticsBatch =
        add(AnalyticsEvent(type, params.mapNotNull { createParam(it.first, it.second) }))

    /** Emit every buffered event, then clear the buffer. */
    fun flush() {
        pending.forEach(analytics::logEvent)
        pending.clear()
    }

    /** Number of events waiting to be flushed. */
    val size: Int get() = pending.size
}

/** Create an [AnalyticsBatch] for this helper. */
fun AnalyticsHelper.batch(): AnalyticsBatch = AnalyticsBatch(this)
