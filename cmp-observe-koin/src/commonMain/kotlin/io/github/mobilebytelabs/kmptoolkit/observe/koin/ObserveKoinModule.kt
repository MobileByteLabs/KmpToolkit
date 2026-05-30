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
import io.github.mobilebytelabs.kmptoolkit.observe.hooks.FirebaseAnalyticsHealthHook
import io.github.mobilebytelabs.kmptoolkit.observe.hooks.FirebaseCrashlyticsAttributionHook
import io.github.mobilebytelabs.kmptoolkit.observe.hooks.FirebasePerformanceHook
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Zero-config Koin module for cmp-observe.
 *
 * Reads consent state + per-library `observability_opt_in` flags (consumer-side
 * Phase 04 wiring) and registers only the hooks the consumer + end-user have
 * agreed to.
 *
 * Default tier posture per GOAL.md D8:
 *   - T0 (Crashlytics tagging) → ON  (operational/security parity with existing Crashlytics defaults)
 *   - T1 (init health events) → ON  (analytics_enabled gate inherited from consumer)
 *   - T2 (lifecycle events)   → OFF (consumer-level opt-in via Settings → Privacy)
 *   - T3 (perf traces)        → OFF (consumer-level opt-in)
 *   - T4 (full API usage)     → OFF (consumer-level + per-end-user opt-in + iOS ATT)
 *
 * Usage in consumer app's Koin startup:
 * ```kotlin
 * startKoin {
 *     modules(
 *         observeKoinModule(
 *             enableT0 = true,
 *             enableT1 = true,
 *             enableT2 = consentState.t2LifecycleEvents,  // from Settings → Privacy
 *             enableT3 = consentState.t3PerformanceTraces,
 *         ),
 *     )
 * }
 * // Then at app onCreate:
 * getKoin().getAll<LibraryObservationHook>().forEach { LibraryObservation.register(it) }
 * ```
 *
 * SupabaseEventsHook is NOT auto-wired — it requires per-consumer anon-key +
 * supabase URL + httpClient + coroutineScope; consumer apps instantiate it
 * explicitly and call LibraryObservation.register() themselves.
 *
 * Authored 2026-05-30 by library-runtime-observability epic Phase 01 T9.
 */
public fun observeKoinModule(
    enableT0: Boolean = true,
    enableT1: Boolean = true,
    enableT3: Boolean = false,
): Module = module {
    if (enableT0) single<LibraryObservationHook>(qualifier = org.koin.core.qualifier.named("T0")) {
        FirebaseCrashlyticsAttributionHook()
    }
    if (enableT1) single<LibraryObservationHook>(qualifier = org.koin.core.qualifier.named("T1")) {
        FirebaseAnalyticsHealthHook()
    }
    if (enableT3) single<LibraryObservationHook>(qualifier = org.koin.core.qualifier.named("T3")) {
        FirebasePerformanceHook()
    }
}
