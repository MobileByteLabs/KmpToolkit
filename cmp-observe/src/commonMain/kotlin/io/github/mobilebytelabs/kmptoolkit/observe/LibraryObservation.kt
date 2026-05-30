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

/**
 * Process-wide hook registry. Thread-safe via copy-on-write list.
 *
 * notify* methods fan-out to all registered hooks. Hook exceptions are caught
 * and discarded (per [LibraryObservationHook] contract); a misbehaving hook
 * cannot crash the host application or block subsequent hooks.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T3.
 */
public object LibraryObservation {
    @Volatile
    private var hooks: List<LibraryObservationHook> = emptyList()
    private val lock = Any()

    /** Register a hook. Idempotent — registering the same hook twice still fans out twice. */
    public fun register(hook: LibraryObservationHook) {
        synchronized(lock) { hooks = hooks + hook }
    }

    /**
     * Atomically swap the registered hook list. Used by Phase 04 (consent observers)
     * to re-register hooks when user toggles per-tier consent in Settings → Privacy.
     */
    public fun replaceHooks(transform: (List<LibraryObservationHook>) -> List<LibraryObservationHook>) {
        synchronized(lock) { hooks = transform(hooks) }
    }

    public fun notifyInit(meta: CmpMetadata) {
        hooks.forEach { safeCall { it.onInitStart(meta) } }
    }

    public fun notifyInitComplete(meta: CmpMetadata) {
        hooks.forEach { safeCall { it.onInitComplete(meta) } }
    }

    public fun notifyInitFailure(meta: CmpMetadata, throwable: Throwable) {
        hooks.forEach { safeCall { it.onInitFailure(meta, throwable) } }
    }

    public fun notifyLifecycle(meta: CmpMetadata, event: String, payload: Map<String, Any?> = emptyMap()) {
        hooks.forEach { safeCall { it.onLifecycleEvent(meta, event, payload) } }
    }

    public fun notifyClose(meta: CmpMetadata) {
        hooks.forEach { safeCall { it.onClose(meta) } }
    }

    /** Test-only: clear all hooks. NOT public API. */
    internal fun reset() {
        synchronized(lock) { hooks = emptyList() }
    }

    private inline fun safeCall(block: () -> Unit) {
        try {
            block()
        } catch (_: Throwable) {
            // Hook errors isolated — never propagate to caller.
        }
    }
}
