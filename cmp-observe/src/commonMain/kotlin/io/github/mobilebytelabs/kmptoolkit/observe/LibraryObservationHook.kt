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
 * Hook interface for library observability signal capture.
 *
 * Implementations attach to [LibraryObservation] at app startup to receive
 * library lifecycle events. Each library (cmp-*, worker-kmp, monetization-kmp,
 * paycraft) calls notify* methods at its init / lifecycle / close points;
 * registered hooks fan-out the notification to their respective backends
 * (Crashlytics, Firebase Analytics, Firebase Performance, framework-supabase).
 *
 * Contract (per [LibraryObservation]):
 * - Hooks MUST NOT throw — exceptions are caught + isolated by [LibraryObservation.safeCall].
 * - Hooks MUST NOT block — long-running work (network POST, DB write) MUST be off-main-thread.
 * - Hooks MAY ignore any notification (no-op is a valid impl).
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T2.
 */
public interface LibraryObservationHook {
    public fun onInitStart(meta: CmpMetadata)
    public fun onInitComplete(meta: CmpMetadata)
    public fun onInitFailure(meta: CmpMetadata, throwable: Throwable)
    public fun onLifecycleEvent(meta: CmpMetadata, event: String, payload: Map<String, Any?>)
    public fun onClose(meta: CmpMetadata)
}

/**
 * Library metadata populated at compile time by per-library Gradle task
 * (see Phase 02 — CMP_LIBRARY_METADATA.gradle).
 *
 * @property name    Module short-name, e.g. "cmp-share".
 * @property version Semantic version, e.g. "3.2.11".
 * @property artifact Maven artifact ID, e.g. "io.github.mobilebytelabs:cmp-share".
 */
public data class CmpMetadata(public val name: String, public val version: String, public val artifact: String)
