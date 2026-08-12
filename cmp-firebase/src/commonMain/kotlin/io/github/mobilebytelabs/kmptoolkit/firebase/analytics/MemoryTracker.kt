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
 * Samples memory usage and emits a `memory_usage` event, warning when usage crosses
 * [warnThresholdBytes]. Platform-agnostic by design: pass a [usedMemoryBytes] provider
 * (e.g. `Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() }` on JVM/Android),
 * so no `expect`/`actual` sprawl is needed and unsupported platforms simply no-op.
 *
 * ```kotlin
 * val mem = MemoryTracker(analytics) { Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() } }
 * mem.logMemoryUsage("after_data_load")
 * ```
 */
class MemoryTracker(
    private val analytics: AnalyticsHelper,
    private val warnThresholdBytes: Long = DEFAULT_WARN_BYTES,
    private val usedMemoryBytes: () -> Long? = { null },
) {
    /** Sample memory now and log it (tagged with [tag]); no-op if the provider returns null. */
    fun logMemoryUsage(tag: String) {
        val used = usedMemoryBytes() ?: return
        val level = if (used >= warnThresholdBytes) "high" else "normal"
        analytics.logEvent(
            AnalyticsEvent(
                EVENT_MEMORY,
                listOf(
                    Param(ParamKeys.FEATURE_NAME, tag),
                    Param(PARAM_USED_BYTES, used.toString()),
                    Param(PARAM_LEVEL, level),
                ),
            ),
        )
    }

    private companion object {
        const val EVENT_MEMORY = "memory_usage"
        const val PARAM_USED_BYTES = "used_bytes"
        const val PARAM_LEVEL = "memory_level"
        const val DEFAULT_WARN_BYTES = 256L * 1024 * 1024 // 256 MB
    }
}
