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
 * The KMP target this code is running on.
 *
 * One of: `"android"`, `"ios"`, `"macos"`, `"tvos"`, `"watchos"`, `"jvm"`,
 * `"js"`, `"linux"`, `"mingw"`, `"wasmjs"`, `"wasmwasi"`.
 *
 * Auto-injected on every event by [FirebaseAnalyticsHelper],
 * [MeasurementProtocolAnalyticsHelper], and [StubAnalyticsHelper] under the
 * key [ParamKeys.PLATFORM] (`"kmp_platform"`). This disambiguates events in
 * BigQuery — without it, a `screen_view` event is just a `screen_view` whether
 * it came from Android, iOS, watchOS, or Linux.
 *
 * Why a separate `kmp_platform` (not GA4's built-in `platform`):
 * - GA4 native auto-`platform` is coarse: `"android"` / `"ios"` / `"web"` only
 * - MP HTTP events don't get auto-`platform` unless we set it
 * - Sub-platforms (watchOS vs iOS vs macOS — all "Apple") can't be told apart
 *   in GA4's built-in field
 *
 * Apps can override per-helper:
 * ```kotlin
 * FirebaseAnalyticsHelper(Firebase.analytics, platformOverride = "android-tv")
 * ```
 * or per-event by manually setting the param — auto-injection respects existing values:
 * ```kotlin
 * analytics.logEvent(EventTypes.BUTTON_CLICK,
 *     ParamKeys.PLATFORM to "android-tablet",  // takes precedence over kmpPlatform
 *     ParamKeys.BUTTON_NAME to "save",
 * )
 * ```
 */
expect val kmpPlatform: String

/**
 * Returns [event] with the [ParamKeys.PLATFORM] param set to [platform], unless
 * the event already has that param (manual override takes precedence).
 *
 * Internal helper for [AnalyticsHelper] implementations that auto-inject platform.
 */
internal fun AnalyticsEvent.withPlatform(platform: String): AnalyticsEvent =
    if (extras.any { it.key == ParamKeys.PLATFORM }) {
        this
    } else {
        withParam(ParamKeys.PLATFORM, platform)
    }
