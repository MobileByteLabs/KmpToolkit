/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.analytics

import kotlin.time.Clock

/**
 * Lightweight performance timer for analytics. Pairs a `start` with a `stop` and emits
 * a [EventTypes.LOADING_TIME] event with duration in ms.
 *
 * Designed for screen-render time, network call latency, or any operation you want to
 * track. NOT a full APM solution — for production performance monitoring, use
 * Firebase Performance Monitoring directly.
 *
 * @sample
 * ```kotlin
 * val tracker = PerformanceTracker(analytics)
 * val handle = tracker.start("settings_screen_render")
 * // ... render work ...
 * tracker.stop(handle, extras = mapOf(ParamKeys.SCREEN_NAME to "settings"))
 * ```
 */
class PerformanceTracker(private val helper: AnalyticsHelper) {

    /** Opaque handle returned by [start]. Pass to [stop] to emit the event. */
    data class Handle internal constructor(val name: String, val startedAt: Long)

    fun start(name: String): Handle = Handle(
        name = name,
        startedAt = Clock.System.now().toEpochMilliseconds(),
    )

    fun stop(handle: Handle, extras: Map<String, String> = emptyMap()) {
        val durationMs = Clock.System.now().toEpochMilliseconds() - handle.startedAt
        val params = mutableListOf<Param>(
            Param(ParamKeys.FEATURE_NAME, handle.name),
            Param(ParamKeys.LOADING_TIME_MS, durationMs.toString()),
        )
        extras.forEach { (k, v) -> createParam(k, v)?.let(params::add) }
        helper.logEvent(AnalyticsEvent(EventTypes.LOADING_TIME, params))
    }

    /** Time a block — calls [start] and [stop] around it. Returns the block's value. */
    inline fun <T> measure(name: String, block: () -> T): T {
        val handle = start(name)
        return try {
            block()
        } finally {
            stop(handle)
        }
    }
}
