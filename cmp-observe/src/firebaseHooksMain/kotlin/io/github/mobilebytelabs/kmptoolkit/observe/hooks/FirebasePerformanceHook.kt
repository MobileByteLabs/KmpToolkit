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
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
@OptIn(ExperimentalAtomicApi::class)
public class FirebasePerformanceHook : LibraryObservationHook {
    // AtomicReference + CAS retry loop — multiplatform-safe replacement for
    // `synchronized(lock)`. Required because firebaseHooksMain is consumed by
    // both android (where kotlin.jvm.synchronized works) and ios (where it does NOT).
    private val activeTracesRef: AtomicReference<Map<String, Trace>> = AtomicReference(emptyMap())

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
                    updateTraces { it + (traceName to trace) }
                }

                event.endsWith("_end") -> {
                    val prefix = event.removeSuffix("_end")
                    val traceName = "${meta.name}_$prefix"
                    val trace = removeTrace(traceName)
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
            val prefix = "${meta.name}_"
            val current = activeTracesRef.load()
            val toRemove = current.filterKeys { it.startsWith(prefix) }
            if (toRemove.isEmpty()) return@runCatching
            updateTraces { it - toRemove.keys }
            toRemove.values.forEach { it.stop() }
        }
    }

    private inline fun updateTraces(transform: (Map<String, Trace>) -> Map<String, Trace>) {
        while (true) {
            val current = activeTracesRef.load()
            val next = transform(current)
            if (activeTracesRef.compareAndSet(current, next)) return
        }
    }

    private fun removeTrace(name: String): Trace? {
        while (true) {
            val current = activeTracesRef.load()
            val trace = current[name] ?: return null
            if (activeTracesRef.compareAndSet(current, current - name)) return trace
        }
    }
}
