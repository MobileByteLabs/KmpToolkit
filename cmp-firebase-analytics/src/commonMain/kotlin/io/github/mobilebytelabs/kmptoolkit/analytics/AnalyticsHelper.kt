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
 * Platform-agnostic interface for logging analytics events.
 *
 * Implementations:
 * - [StubAnalyticsHelper]: dev/debug — logs to Kermit
 * - [NoOpAnalyticsHelper]: tests/previews — does nothing
 * - User-supplied: e.g., a Firebase Analytics adapter using GitLive Firebase Kotlin SDK.
 *   The library does NOT bundle a backend; users wire their own. See README for the
 *   `FirebaseAnalyticsHelper` template.
 *
 * @sample
 * ```kotlin
 * // Inject via Koin
 * val analytics: AnalyticsHelper = koinInject()
 *
 * // Direct logging
 * analytics.logEvent(EventTypes.BUTTON_CLICK,
 *     ParamKeys.BUTTON_NAME to "save",
 *     ParamKeys.SCREEN_NAME to "settings"
 * )
 *
 * // Builder pattern
 * val event = AnalyticsEvent(EventTypes.FORM_COMPLETED)
 *     .withParam(ParamKeys.FORM_NAME, "registration")
 *     .withParam(ParamKeys.COMPLETION_TIME, "45s")
 * analytics.logEvent(event)
 * ```
 */
interface AnalyticsHelper {

    /** Core method — sends a validated event to the backend. */
    fun logEvent(event: AnalyticsEvent)

    /** Convenience: log with type + vararg pairs. */
    fun logEvent(type: String, vararg params: Pair<String, String>) {
        logEvent(AnalyticsEvent(type, params.map { Param(it.first, it.second) }))
    }

    /** Convenience: log with type + Map of params. */
    fun logEvent(type: String, params: Map<String, String>) {
        logEvent(AnalyticsEvent(type, params.map { Param(it.key, it.value) }))
    }

    /** Convenience: screen view. */
    fun logScreenView(screenName: String, sourceScreen: String? = null) {
        val params = mutableListOf(Param(ParamKeys.SCREEN_NAME, screenName))
        sourceScreen?.let { params.add(Param(ParamKeys.SOURCE_SCREEN, it)) }
        logEvent(AnalyticsEvent(EventTypes.SCREEN_VIEW, params))
    }

    /** Convenience: button click. */
    fun logButtonClick(buttonName: String, screenName: String? = null) {
        val params = mutableListOf(Param(ParamKeys.BUTTON_NAME, buttonName))
        screenName?.let { params.add(Param(ParamKeys.SCREEN_NAME, it)) }
        logEvent(AnalyticsEvent(EventTypes.BUTTON_CLICK, params))
    }

    /** Convenience: error event. */
    fun logError(errorMessage: String, errorCode: String? = null, screen: String? = null) {
        val params = mutableListOf(Param(ParamKeys.ERROR_MESSAGE, errorMessage))
        errorCode?.let { params.add(Param(ParamKeys.ERROR_CODE, it)) }
        screen?.let { params.add(Param(ParamKeys.SCREEN_NAME, it)) }
        logEvent(AnalyticsEvent(EventTypes.ERROR_OCCURRED, params))
    }

    /** Convenience: state transition. */
    fun logStateTransition(screenName: String, from: String, to: String) {
        logEvent(
            AnalyticsEvent(
                EventTypes.STATE_TRANSITIONED,
                listOf(
                    Param(ParamKeys.SCREEN_NAME, screenName),
                    Param(ParamKeys.FROM_STATE, from),
                    Param(ParamKeys.TO_STATE, to),
                ),
            ),
        )
    }

    /** Convenience: feature usage. */
    fun logFeatureUsed(featureName: String, screen: String? = null) {
        val params = mutableListOf(Param(ParamKeys.FEATURE_NAME, featureName))
        screen?.let { params.add(Param(ParamKeys.SCREEN_NAME, it)) }
        logEvent(AnalyticsEvent(EventTypes.FEATURE_USED, params))
    }

    /**
     * Set a user property. Default: no-op. Override in implementations that support it.
     * Firebase constraints: name ≤ 24 chars, value ≤ 36 chars.
     */
    fun setUserProperty(name: String, value: String) {}

    /**
     * Set the user ID for cross-session tracking. Default: no-op.
     * NEVER pass PII — use a hashed/obfuscated identifier.
     */
    fun setUserId(userId: String) {}
}
