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
 * Non-Firebase tier actual: watchOS (×4) · Linux (×2) · mingwX64 · wasmJs · wasmWasi.
 *
 * GitLive Firebase Analytics does not ship on these 10 targets, so the default
 * helper is [NoOpAnalyticsHelper]. Apps that want event capture on these
 * platforms (recommended) construct [io.github.mobilebytelabs.kmptoolkit.analytics.mp.MeasurementProtocolAnalyticsHelper]
 * directly in their DI module — events land in the same Firebase Analytics
 * property + same BigQuery dataset as native SDK events.
 *
 * Why MP isn't auto-wired here: it requires app-specific configuration
 * ([MpConfig] with measurement_id + api_secret) that this library cannot
 * load on its own. The factory below stays NoOp; apps wire MP explicitly.
 *
 * Recommended setup (in your app's Koin module):
 *
 * ```kotlin
 * import com.russhwolf.settings.Settings
 * import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MeasurementProtocolAnalyticsHelper
 * import io.github.mobilebytelabs.kmptoolkit.analytics.mp.MpConfig
 *
 * val analyticsModule = module {
 *     single<AnalyticsHelper> {
 *         MeasurementProtocolAnalyticsHelper(
 *             config = MpConfig(
 *                 measurementId = "G-XXXXXXXX",
 *                 apiSecret     = SecureStore.read("MP_API_SECRET"), // load from secrets store
 *             ),
 *             settings = Settings(),
 *         )
 *     }
 * }
 * ```
 */
actual fun provideAnalyticsHelper(): AnalyticsHelper = NoOpAnalyticsHelper
