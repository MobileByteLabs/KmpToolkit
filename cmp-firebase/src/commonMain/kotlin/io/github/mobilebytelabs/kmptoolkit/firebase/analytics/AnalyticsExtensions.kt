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
 * Compose-friendly DSL for building events.
 *
 * @sample
 * ```kotlin
 * analytics.log(EventTypes.FORM_COMPLETED) {
 *     param(ParamKeys.FORM_NAME, "registration")
 *     param(ParamKeys.COMPLETION_TIME, "45s")
 * }
 * ```
 */
inline fun AnalyticsHelper.log(type: String, builder: AnalyticsEventBuilder.() -> Unit = {}) {
    val b = AnalyticsEventBuilder(type)
    b.builder()
    logEvent(b.build())
}

class AnalyticsEventBuilder(private val type: String) {
    private val params = mutableListOf<Param>()

    /** Add a string param. Silently skips invalid keys/values rather than throwing. */
    fun param(key: String, value: String) {
        createParam(key, value)?.let(params::add)
    }

    /** Add a numeric param (converted to String). */
    fun param(key: String, value: Number) = param(key, value.toString())

    /** Add a boolean param. */
    fun param(key: String, value: Boolean) = param(key, value.toString())

    fun build(): AnalyticsEvent = AnalyticsEvent(type, params.toList())
}
