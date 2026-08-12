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

/**
 * Conversion-funnel helper. Emits `funnel_start` / `funnel_step` / `funnel_complete` /
 * `funnel_abandon` events with a consistent `funnel` param, so GA4 funnel/path explorations
 * work out of the box:
 *
 * ```kotlin
 * val f = analytics.funnel("onboarding")
 * f.start()
 * f.step("enter_phone")
 * f.step("verify_otp")
 * f.complete()   // or f.abandon("otp_timeout")
 * ```
 */
class Funnel internal constructor(
    private val analytics: AnalyticsHelper,
    private val name: String,
) {
    fun start() = emit(EVENT_START)

    fun step(step: String) = analytics.logEvent(
        AnalyticsEvent(EVENT_STEP, listOf(Param(PARAM_FUNNEL, name), Param(PARAM_STEP, step))),
    )

    fun complete() = emit(EVENT_COMPLETE)

    fun abandon(reason: String? = null) {
        val params = buildList {
            add(Param(PARAM_FUNNEL, name))
            reason?.let { add(Param(PARAM_REASON, it)) }
        }
        analytics.logEvent(AnalyticsEvent(EVENT_ABANDON, params))
    }

    private fun emit(type: String) =
        analytics.logEvent(AnalyticsEvent(type, listOf(Param(PARAM_FUNNEL, name))))

    private companion object {
        const val EVENT_START = "funnel_start"
        const val EVENT_STEP = "funnel_step"
        const val EVENT_COMPLETE = "funnel_complete"
        const val EVENT_ABANDON = "funnel_abandon"
        const val PARAM_FUNNEL = "funnel"
        const val PARAM_STEP = "step"
        const val PARAM_REASON = "reason"
    }
}

/** Start a conversion [Funnel] named [name]. */
fun AnalyticsHelper.funnel(name: String): Funnel = Funnel(this, name)
