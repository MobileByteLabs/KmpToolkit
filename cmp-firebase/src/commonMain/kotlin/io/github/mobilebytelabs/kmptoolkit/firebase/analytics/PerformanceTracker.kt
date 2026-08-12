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
 * Performance timer + percentile analyzer for analytics.
 *
 * Pairs a [start] with a [stop] and emits a [EventTypes.LOADING_TIME] event with the
 * duration in ms. On top of the basic timing it accumulates per-operation durations so you
 * can pull **P50/P95/P99** stats ([getPerformanceStats]) and auto-classifies each timing as
 * `fast | slow | very_slow` against [slowThresholdMs] / [verySlowThresholdMs] (surfaced as
 * a `performance_level` param), so a regression is visible in the dashboard.
 *
 * NOT a full APM — for production tracing use Firebase Performance Monitoring directly.
 *
 * @sample
 * ```kotlin
 * val tracker = PerformanceTracker(analytics)
 * val handle = tracker.start("settings_screen_render")
 * // ... render work ...
 * tracker.stop(handle, extras = mapOf(ParamKeys.SCREEN_NAME to "settings"))
 * val stats = tracker.getPerformanceStats("settings_screen_render")  // p95Ms, p99Ms, ...
 * ```
 */
class PerformanceTracker(
    private val helper: AnalyticsHelper,
    private val slowThresholdMs: Long = SLOW_MS,
    private val verySlowThresholdMs: Long = VERY_SLOW_MS,
) {

    /** Opaque handle returned by [start]. Pass to [stop] to emit the event. */
    data class Handle internal constructor(val name: String, val startedAt: Long)

    private val samples = mutableMapOf<String, MutableList<Long>>()

    fun start(name: String): Handle = Handle(
        name = name,
        startedAt = Clock.System.now().toEpochMilliseconds(),
    )

    fun stop(handle: Handle, extras: Map<String, String> = emptyMap()) {
        val durationMs = Clock.System.now().toEpochMilliseconds() - handle.startedAt
        samples.getOrPut(handle.name) { mutableListOf() }.add(durationMs)

        val params = mutableListOf(
            Param(ParamKeys.FEATURE_NAME, handle.name),
            Param(ParamKeys.LOADING_TIME_MS, durationMs.toString()),
            Param(PARAM_PERFORMANCE_LEVEL, performanceLevel(durationMs)),
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

    /**
     * Percentile stats for [operationName] across every [stop] recorded so far, or `null`
     * if the operation was never timed.
     */
    fun getPerformanceStats(operationName: String): PerformanceStats? {
        val durations = samples[operationName]?.takeIf { it.isNotEmpty() } ?: return null
        val sorted = durations.sorted()
        return PerformanceStats(
            operationName = operationName,
            count = sorted.size,
            averageMs = sorted.average(),
            medianMs = percentile(sorted, 0.50),
            p95Ms = percentile(sorted, 0.95),
            p99Ms = percentile(sorted, 0.99),
            minMs = sorted.first().toDouble(),
            maxMs = sorted.last().toDouble(),
        )
    }

    /** Emit a [PerformanceStats] snapshot as an analytics event (percentiles + count). */
    fun logPerformanceSummary(operationName: String) {
        val s = getPerformanceStats(operationName) ?: return
        helper.logEvent(
            AnalyticsEvent(
                EventTypes.LOADING_TIME,
                listOf(
                    Param(ParamKeys.FEATURE_NAME, "${operationName}_summary"),
                    Param("count", s.count.toString()),
                    Param("p50_ms", s.medianMs.toString()),
                    Param("p95_ms", s.p95Ms.toString()),
                    Param("p99_ms", s.p99Ms.toString()),
                ),
            ),
        )
    }

    /** Clear accumulated samples (e.g. between sessions). */
    fun clearMetrics() = samples.clear()

    private fun performanceLevel(durationMs: Long): String = when {
        durationMs >= verySlowThresholdMs -> "very_slow"
        durationMs >= slowThresholdMs -> "slow"
        else -> "fast"
    }

    private fun percentile(sorted: List<Long>, fraction: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val rank = fraction * (sorted.size - 1)
        val lo = rank.toInt()
        val hi = minOf(lo + 1, sorted.size - 1)
        val weight = rank - lo
        return sorted[lo] * (1 - weight) + sorted[hi] * weight
    }

    private companion object {
        const val SLOW_MS = 1000L
        const val VERY_SLOW_MS = 5000L
        const val PARAM_PERFORMANCE_LEVEL = "performance_level"
    }
}

/** Percentile snapshot of the durations recorded for one operation. */
data class PerformanceStats(
    val operationName: String,
    val count: Int,
    val averageMs: Double,
    val medianMs: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val minMs: Double,
    val maxMs: Double,
)
