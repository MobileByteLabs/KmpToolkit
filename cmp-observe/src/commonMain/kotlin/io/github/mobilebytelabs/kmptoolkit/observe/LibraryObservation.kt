/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Process-wide hook registry. Thread-safe via copy-on-write [AtomicReference].
 *
 * notify* methods fan-out to all registered hooks. Hook exceptions are caught
 * and discarded (per [LibraryObservationHook] contract); a misbehaving hook
 * cannot crash the host application or block subsequent hooks.
 *
 * Uses [AtomicReference] (kotlin.concurrent.atomics — stdlib, multiplatform)
 * instead of `@Volatile` + `synchronized` so the same code compiles for all
 * 10 KMP targets that cmp-observe ships (jvm/android/ios/macos/js/wasmJs/
 * tvos/watchos/linux/mingw).
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T3.
 */
@OptIn(ExperimentalAtomicApi::class)
public object LibraryObservation {
    private val hooksRef: AtomicReference<List<LibraryObservationHook>> = AtomicReference(emptyList())

    /** Register a hook. Idempotent — registering the same hook twice still fans out twice. */
    public fun register(hook: LibraryObservationHook) {
        updateHooks { it + hook }
    }

    /**
     * Atomically swap the registered hook list. Used by Phase 04 (consent observers)
     * to re-register hooks when user toggles per-tier consent in Settings → Privacy.
     */
    public fun replaceHooks(transform: (List<LibraryObservationHook>) -> List<LibraryObservationHook>) {
        updateHooks(transform)
    }

    public fun notifyInit(meta: CmpMetadata) {
        hooksRef.load().forEach { safeCall { it.onInitStart(meta) } }
    }

    public fun notifyInitComplete(meta: CmpMetadata) {
        hooksRef.load().forEach { safeCall { it.onInitComplete(meta) } }
    }

    public fun notifyInitFailure(meta: CmpMetadata, throwable: Throwable) {
        hooksRef.load().forEach { safeCall { it.onInitFailure(meta, throwable) } }
    }

    public fun notifyLifecycle(meta: CmpMetadata, event: String, payload: Map<String, Any?> = emptyMap()) {
        hooksRef.load().forEach { safeCall { it.onLifecycleEvent(meta, event, payload) } }
    }

    public fun notifyClose(meta: CmpMetadata) {
        hooksRef.load().forEach { safeCall { it.onClose(meta) } }
    }

    /** Test-only: clear all hooks. NOT public API. */
    internal fun reset() {
        hooksRef.store(emptyList())
    }

    private inline fun updateHooks(transform: (List<LibraryObservationHook>) -> List<LibraryObservationHook>) {
        while (true) {
            val current = hooksRef.load()
            val next = transform(current)
            if (hooksRef.compareAndSet(current, next)) return
        }
    }

    private inline fun safeCall(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Hook errors isolated — never propagate to caller.
        }
    }
}
