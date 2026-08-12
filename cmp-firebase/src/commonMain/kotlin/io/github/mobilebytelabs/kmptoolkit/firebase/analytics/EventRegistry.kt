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
 * Per-app registry of declared event types. Populated by code generators
 * (e.g., framework `/idea export-analytics` reading screen YAML).
 *
 * Apps that want strict event taxonomy can install a registry and reject
 * unregistered events at runtime in debug builds. Production code should NOT
 * use the registry on the hot path — taxonomy is enforced at codegen time.
 *
 * @sample
 * ```kotlin
 * // Auto-generated:
 * val SettingsEventRegistry = EventRegistry(
 *     "settings",
 *     events = setOf("settings_screen_viewed", "settings_state_transitioned", "settings_save_clicked"),
 *     params = mapOf(
 *         "settings_state_transitioned" to setOf("from_state", "to_state"),
 *     ),
 * )
 *
 * // App startup:
 * val analytics = if (BuildConfig.DEBUG) {
 *     ValidatingAnalyticsHelper(realHelper, SettingsEventRegistry)
 * } else {
 *     realHelper
 * }
 * ```
 */
data class EventRegistry(
    /** Logical scope name — e.g. "settings", "checkout". */
    val scope: String,
    /** Declared event types within this scope. */
    val events: Set<String>,
    /** Optional declared params per event. Keys not in this map are unconstrained. */
    val params: Map<String, Set<String>> = emptyMap(),
) {
    /** Returns true if [eventType] is declared in this registry. */
    fun contains(eventType: String): Boolean = eventType in events

    /** Returns true if [paramKey] is declared for [eventType] (true if unconstrained). */
    fun allowsParam(eventType: String, paramKey: String): Boolean = params[eventType]?.let { paramKey in it } ?: true
}

/**
 * Wraps an [AnalyticsHelper], validating each event against an [EventRegistry].
 * Use only in debug/test builds — adds a small overhead per log call.
 */
class RegistryValidatingHelper(
    private val delegate: AnalyticsHelper,
    private val registry: EventRegistry,
    private val onViolation: (String) -> Unit = { /* default: silent */ },
) : AnalyticsHelper {

    override fun logEvent(event: AnalyticsEvent) {
        if (!registry.contains(event.type)) {
            onViolation("Event '${event.type}' not in registry '${registry.scope}'")
        }
        for (extra in event.extras) {
            if (!registry.allowsParam(event.type, extra.key)) {
                onViolation("Param '${extra.key}' not declared for event '${event.type}'")
            }
        }
        delegate.logEvent(event)
    }

    override fun setUserProperty(name: String, value: String) = delegate.setUserProperty(name, value)

    override fun setUserId(userId: String) = delegate.setUserId(userId)
}
