/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import kotlin.time.Clock

/**
 * Tracks app-launch timing and background/foreground transitions — the "behaviour" signals
 * an analytics dashboard uses for engagement and cold-start performance.
 *
 * Wire it once at app startup:
 * ```kotlin
 * val lifecycle = AppLifecycleTracker(analytics)
 * // as early as possible (e.g. Application.onCreate / @main init):
 * lifecycle.markAppLaunchStart()
 * // when the first frame / home screen is ready:
 * lifecycle.markAppLaunchComplete()
 * // from your platform lifecycle observer:
 * lifecycle.onEnterBackground(); lifecycle.onEnterForeground()
 * ```
 */
class AppLifecycleTracker(private val analytics: AnalyticsHelper) {

    private var launchStartedAt: Long? = null
    private var backgroundedAt: Long? = null

    /** Call as early as possible in the launch path. */
    fun markAppLaunchStart() {
        launchStartedAt = now()
    }

    /** Call when the app is interactive (first frame / home ready). Emits `app_launch`. */
    fun markAppLaunchComplete() {
        val start = launchStartedAt ?: return
        val durationMs = now() - start
        launchStartedAt = null
        analytics.logEvent(
            AnalyticsEvent(
                EventTypes.APP_LAUNCH,
                listOf(
                    Param(ParamKeys.LOADING_TIME_MS, durationMs.toString()),
                    Param("launch_type", "cold"),
                ),
            ),
        )
    }

    /** Call when the app moves to background. Records the timestamp for the next foreground. */
    fun onEnterBackground() {
        backgroundedAt = now()
        analytics.logEvent(AnalyticsEvent(EVENT_BACKGROUND))
    }

    /** Call when the app returns to foreground. Emits `app_foreground` with the time away. */
    fun onEnterForeground() {
        val awayMs = backgroundedAt?.let { now() - it }
        backgroundedAt = null
        val params = buildList {
            awayMs?.let { add(Param("time_away_ms", it.toString())) }
        }
        analytics.logEvent(AnalyticsEvent(EVENT_FOREGROUND, params))
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val EVENT_BACKGROUND = "app_background"
        const val EVENT_FOREGROUND = "app_foreground"
    }
}
