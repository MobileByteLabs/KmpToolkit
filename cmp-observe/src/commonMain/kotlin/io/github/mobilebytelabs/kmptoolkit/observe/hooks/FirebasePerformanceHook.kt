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

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.perf.metrics.Trace
import dev.gitlive.firebase.perf.performance
import io.github.mobilebytelabs.kmptoolkit.observe.CmpMetadata
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook

/**
 * T3 hook: wraps lifecycle events whose name ends with `_start` / `_end` in
 * Firebase Performance Traces.
 *
 * Convention: `share_invoke_start` opens a trace named `cmp-share_share_invoke`;
 * `share_invoke_end` closes it. Mismatched `_end` without prior `_start` are
 * silently dropped.
 *
 * Trace name format: `{module_name}_{event_prefix}` (event prefix = event name minus suffix).
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T6.
 */
public class FirebasePerformanceHook : LibraryObservationHook {
    private val activeTraces: MutableMap<String, Trace> = mutableMapOf()
    private val lock = Any()

    override fun onInitStart(meta: CmpMetadata) {}
    override fun onInitComplete(meta: CmpMetadata) {}
    override fun onInitFailure(meta: CmpMetadata, throwable: Throwable) {}

    override fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>) {
        runCatching {
            when {
                event.endsWith("_start") -> {
                    val prefix = event.removeSuffix("_start")
                    val traceName = "${meta.name}_$prefix"
                    val trace = Firebase.performance.newTrace(traceName).apply { start() }
                    synchronized(lock) { activeTraces[traceName] = trace }
                }
                event.endsWith("_end") -> {
                    val prefix = event.removeSuffix("_end")
                    val traceName = "${meta.name}_$prefix"
                    val trace = synchronized(lock) { activeTraces.remove(traceName) }
                    trace?.stop()
                }
                else -> {
                    // Non-start/_end lifecycle events ignored by T3.
                }
            }
        }
    }

    override fun onClose(meta: CmpMetadata) {
        // Close + drop any traces still open for this module on shutdown.
        runCatching {
            synchronized(lock) {
                val prefix = "${meta.name}_"
                val toRemove = activeTraces.keys.filter { it.startsWith(prefix) }
                toRemove.forEach { activeTraces.remove(it)?.stop() }
            }
        }
    }
}
