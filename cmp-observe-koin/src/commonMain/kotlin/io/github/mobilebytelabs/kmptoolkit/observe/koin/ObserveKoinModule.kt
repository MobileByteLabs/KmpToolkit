/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.observe.koin

import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservation
import io.github.mobilebytelabs.kmptoolkit.observe.LibraryObservationHook
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Zero-config Koin companion for cmp-observe.
 *
 * Takes a list of [LibraryObservationHook] instances assembled by the consumer
 * app (typically: `FirebaseCrashlyticsAttributionHook`, `FirebaseAnalyticsHealthHook`,
 * `FirebasePerformanceHook`, `SupabaseEventsHook` — all from cmp-observe, scoped to
 * the platforms where they ship). Registers each via Koin AND wires it into the
 * process-wide [LibraryObservation] registry at first injection.
 *
 * Why this signature accepts hooks as a parameter rather than importing the
 * Firebase impls directly: the Firebase hooks live in cmp-observe's
 * `firebaseHooksMain` source-set (only 2 of 10 targets see them — the GitLive
 * Firebase v2.4.0 deps for Crashlytics/Analytics/Performance only intersect
 * on {android, ios}). Decoupling cmp-observe-koin from those concrete types
 * lets THIS module ship on all 10 targets — consumer apps on jvm/macos/js/
 * wasmJs/tvos/watchos/linux/mingw can still use it, passing whatever hook set
 * is appropriate for those platforms (most likely none from cmp-observe;
 * consumer-provided hooks remain valid).
 *
 * Usage:
 * ```kotlin
 * startKoin {
 *     modules(
 *         observeKoinModule(
 *             hooks = listOf(
 *                 FirebaseCrashlyticsAttributionHook(),     // T0 — android + ios only
 *                 FirebaseAnalyticsHealthHook(),            // T1 — same
 *                 SupabaseEventsHook(supabaseUrl, anonKey, consumerAppId, httpClient, scope),  // T2 — all 10 targets
 *             ),
 *         ),
 *     )
 * }
 * // First `get<LibraryObservation>()` call wires every hook into the registry.
 * ```
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T9;
 * decoupled from Firebase types 2026-05-30 (audit follow-up) so the module
 * can ship to all 10 KMP targets alongside cmp-observe's expanded target set.
 */
public fun observeKoinModule(hooks: List<LibraryObservationHook>): Module = module {
    // Register each hook under a name-qualified single<>.
    hooks.forEach { hook ->
        val qName = hook::class.simpleName ?: "anonymous-hook-${hook.hashCode()}"
        single<LibraryObservationHook>(qualifier = named(qName)) {
            hook.also { LibraryObservation.register(it) }
        }
    }
}
