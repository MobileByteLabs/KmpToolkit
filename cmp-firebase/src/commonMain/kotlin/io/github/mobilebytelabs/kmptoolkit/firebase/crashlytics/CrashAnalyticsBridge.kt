/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.EventTypes
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.ParamKeys

/**
 * Bridges a crash/recorded-exception to analytics as an [EventTypes.APP_CRASH] event.
 *
 * This is what gives you a **single, all-platform crash view**: native Firebase
 * Crashlytics only ingests 6 of the 15 KMP targets, so the crash reporters also
 * emit this event through their analytics sink. On the Measurement-Protocol /
 * GitLive analytics tiers it reaches GA4 for every remaining target (JVM/desktop,
 * JS/web, Linux, Windows, wasm, tvOS), landing all platforms in one GA4/BigQuery
 * table segmentable by [ParamKeys.PLATFORM] — and feedable to Claude.
 *
 * NOTE: the app_crash mirror fires for exceptions routed to `recordException(...)`
 * (a caught error, or a coroutine/global handler wired to it). On the nonFirebaseMain
 * tier (JVM/Linux/Windows/wasm) it only reaches GA4 when an `MpConfig` is configured
 * via `FirebaseKit.initialize(config)`; otherwise the analytics sink is a no-op.
 *
 * PII-safe: only the exception class, fatal flag, and platform are sent — never the
 * raw message or stack (GA4 forbids PII). The full [CrashReport] stays in
 * Crashlytics (native) or the local JSON log (fallback).
 */
internal fun AnalyticsHelper.logCrash(platform: String, exceptionType: String, fatal: Boolean) {
    logEvent(
        EventTypes.APP_CRASH,
        mapOf(
            ParamKeys.PLATFORM to platform,
            ParamKeys.EXCEPTION_TYPE to exceptionType,
            ParamKeys.FATAL to fatal.toString(),
        ),
    )
}
