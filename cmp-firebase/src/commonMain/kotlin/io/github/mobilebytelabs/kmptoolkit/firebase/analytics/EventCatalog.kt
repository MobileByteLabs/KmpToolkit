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
 * Base class for a **typed, compile-checked event catalog** — the library ships the
 * *pattern*, never the events. Each consuming app defines its own catalog so event names
 * and params are validated at compile time and can't drift:
 *
 * ```kotlin
 * object AppEvents : EventCatalog() {
 *     val LoanApplied = def("loan_applied", ParamKeys.FEATURE_NAME)
 *     fun loanApplied(product: String) = LoanApplied(ParamKeys.FEATURE_NAME to product)
 * }
 * analytics.logEvent(AppEvents.loanApplied("home"))
 * ```
 *
 * Domain events live in the app, not here — this base only provides the safe builders.
 */
abstract class EventCatalog {

    /** A declared event: a fixed [type] plus a fixed set of allowed [allowedKeys]. */
    protected fun def(type: String, vararg allowedKeys: String): EventDef =
        EventDef(type, allowedKeys.toSet())

    /** Build an [AnalyticsEvent] from a raw [type] + pairs (validation handled by AnalyticsEvent). */
    protected fun event(type: String, vararg params: Pair<String, String>): AnalyticsEvent =
        AnalyticsEvent(type, params.map { Param(it.first, it.second) })

    /** A compile-time event declaration; invoking it produces a validated [AnalyticsEvent]. */
    protected class EventDef internal constructor(
        val type: String,
        private val allowedKeys: Set<String>,
    ) {
        operator fun invoke(vararg params: Pair<String, String>): AnalyticsEvent {
            require(allowedKeys.isEmpty() || params.all { it.first in allowedKeys }) {
                "Event '$type' allows keys $allowedKeys but got ${params.map { it.first }}"
            }
            return AnalyticsEvent(type, params.map { Param(it.first, it.second) })
        }
    }
}
