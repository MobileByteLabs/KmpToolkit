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

/**
 * Cross-platform default-helper factory.
 *
 * Returns a platform-appropriate [AnalyticsHelper]:
 *
 * | Tier | Targets | Returns |
 * |---|---|---|
 * | **GitLive supports** (11) | Android, JVM, iOS (×3), macOS (×2), tvOS (×3), JS | `FirebaseAnalyticsHelper(Firebase.analytics)` |
 * | **GitLive unavailable** (10) | watchOS (×4), Linux (×2), mingwX64, wasmJs, wasmWasi | [NoOpAnalyticsHelper] |
 *
 * Apps targeting only Firebase-supported platforms get full analytics. Apps
 * spanning unsupported platforms (e.g. an iOS+watchOS combo) get Firebase
 * on iOS and a no-op on watchOS automatically — no expect/actual setup
 * required at the app level.
 *
 * For finer control (debug → Stub, release → Firebase), construct your own
 * helper in your DI module:
 *
 * ```kotlin
 * val analyticsModule = module {
 *     single<AnalyticsHelper> {
 *         when {
 *             BuildConfig.DEBUG -> StubAnalyticsHelper()
 *             else              -> provideAnalyticsHelper()
 *         }
 *     }
 * }
 * ```
 */
expect fun provideAnalyticsHelper(): AnalyticsHelper
