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
 * Type-safe analytics event with validated parameters.
 *
 * Constraints align with Firebase Analytics limits — events validated at construction time.
 *
 * - Event type: non-blank, ≤ 40 characters
 * - Parameter keys: non-blank, ≤ 40 characters
 * - Parameter values: ≤ 100 characters
 * - Maximum 25 parameters per event (Firebase limit)
 *
 * @sample
 * ```kotlin
 * val event = AnalyticsEvent(EventTypes.SCREEN_VIEW)
 *     .withParam(ParamKeys.SCREEN_NAME, "settings")
 *     .withParam(ParamKeys.SOURCE_SCREEN, "home")
 * ```
 */
data class AnalyticsEvent(val type: String, val extras: List<Param> = emptyList()) {
    init {
        require(type.isNotBlank()) { "Event type cannot be blank" }
        require(type.length <= 40) { "Event type cannot exceed 40 characters: '$type'" }
        require(extras.size <= 25) {
            "AnalyticsEvent cannot have more than 25 parameters (Firebase limit). Got ${extras.size}."
        }
    }

    /** Add a single parameter via builder pattern. Returns a new instance. */
    fun withParam(key: String, value: String): AnalyticsEvent = copy(extras = extras + Param(key, value))

    /** Add multiple parameters via vararg pairs. */
    fun withParams(vararg params: Pair<String, String>): AnalyticsEvent =
        copy(extras = extras + params.map { Param(it.first, it.second) })

    /** Add multiple parameters from a Map. */
    fun withParams(params: Map<String, String>): AnalyticsEvent =
        copy(extras = extras + params.map { Param(it.key, it.value) })
}

/**
 * A validated parameter for an analytics event. Validates Firebase Analytics constraints
 * at construction:
 * - key: non-blank, ≤ 40 chars
 * - value: ≤ 100 chars
 *
 * Use [createParam] for safe creation that returns null on invalid input rather than throwing.
 */
data class Param(val key: String, val value: String) {
    init {
        require(key.isNotBlank()) { "Parameter key cannot be blank" }
        require(key.length <= 40) { "Parameter key cannot exceed 40 characters: '$key'" }
        require(value.length <= 100) { "Parameter value cannot exceed 100 characters" }
    }
}

/** Safe factory: returns null on validation failure instead of throwing. */
fun createParam(key: String, value: String): Param? = runCatching { Param(key, value) }.getOrNull()

/**
 * Standard event type constants. Use these for cross-app analytics consistency.
 *
 * Naming convention: `{snake_case_action}` — matches Firebase Analytics built-in event style.
 */
object EventTypes {
    // Navigation
    const val SCREEN_VIEW = "screen_view"
    const val SCREEN_TRANSITION = "screen_transition"

    // User interaction
    const val BUTTON_CLICK = "button_click"
    const val MENU_ITEM_SELECTED = "menu_item_selected"
    const val SEARCH_PERFORMED = "search_performed"
    const val FILTER_APPLIED = "filter_applied"

    // Form lifecycle
    const val FORM_STARTED = "form_started"
    const val FORM_COMPLETED = "form_completed"
    const val FORM_ABANDONED = "form_abandoned"
    const val FIELD_VALIDATION_ERROR = "field_validation_error"

    // Content
    const val CONTENT_VIEW = "content_view"
    const val CONTENT_SHARED = "content_shared"
    const val CONTENT_LIKED = "content_liked"

    // Errors
    const val ERROR_OCCURRED = "error_occurred"

    /**
     * A crash / recorded exception, bridged to analytics so EVERY KMP target —
     * including the desktop/web/linux/wasm targets Firebase Crashlytics cannot
     * ingest — lands in ONE GA4/BigQuery view, segmentable by [ParamKeys.PLATFORM].
     * Emitted by the crash reporters via their analytics sink alongside (native)
     * Crashlytics. Custom name (not GA4's reserved auto `app_exception`).
     */
    const val APP_CRASH = "app_crash"
    const val API_ERROR = "api_error"
    const val NETWORK_ERROR = "network_error"

    // Performance
    const val APP_LAUNCH = "app_launch"
    const val APP_BACKGROUND = "app_background"
    const val APP_FOREGROUND = "app_foreground"
    const val LOADING_TIME = "loading_time"
    const val STATE_TRANSITIONED = "state_transitioned"

    // Auth
    const val LOGIN_ATTEMPT = "login_attempt"
    const val LOGIN_SUCCESS = "login_success"
    const val LOGIN_FAILURE = "login_failure"
    const val LOGOUT = "logout"
    const val SIGNUP_ATTEMPT = "signup_attempt"
    const val SIGNUP_SUCCESS = "signup_success"

    // Feature usage
    const val FEATURE_USED = "feature_used"
    const val TUTORIAL_STARTED = "tutorial_started"
    const val TUTORIAL_COMPLETED = "tutorial_completed"
    const val TUTORIAL_SKIPPED = "tutorial_skipped"
}

/**
 * Standard parameter keys. Use these for consistent param naming across apps.
 */
object ParamKeys {
    // Screen + navigation
    const val SCREEN_NAME = "screen_name"
    const val SOURCE_SCREEN = "source_screen"
    const val DESTINATION_SCREEN = "destination_screen"

    // Interaction
    const val BUTTON_NAME = "button_name"
    const val ELEMENT_ID = "element_id"
    const val ELEMENT_TYPE = "element_type"
    const val ACTION_TYPE = "action_type"

    // Content
    const val CONTENT_TYPE = "content_type"
    const val CONTENT_ID = "content_id"
    const val CONTENT_NAME = "content_name"
    const val CATEGORY = "category"

    // Search + filters
    const val SEARCH_TERM = "search_term"
    const val FILTER_TYPE = "filter_type"
    const val FILTER_VALUE = "filter_value"
    const val RESULT_COUNT = "result_count"

    // Forms
    const val FORM_NAME = "form_name"
    const val FIELD_NAME = "field_name"
    const val ERROR_MESSAGE = "error_message"
    const val COMPLETION_TIME = "completion_time"

    // Performance
    const val LOADING_TIME_MS = "loading_time_ms"
    const val ERROR_CODE = "error_code"

    // Crash bridge (see EventTypes.APP_CRASH) — no raw message/stack (PII-safe for GA4);
    // full detail stays in Crashlytics (native) / the local JSON log (fallback).
    const val EXCEPTION_TYPE = "exception_type"
    const val FATAL = "fatal"
    const val API_ENDPOINT = "api_endpoint"
    const val NETWORK_TYPE = "network_type"
    const val FROM_STATE = "from_state"
    const val TO_STATE = "to_state"

    // User
    const val USER_ID = "user_id"
    const val USER_TYPE = "user_type"
    const val DEVICE_TYPE = "device_type"
    const val APP_VERSION = "app_version"

    // Auto-injected by helpers — distinguishes events by KMP target.
    // Named `kmp_platform` (not just `platform`) to avoid collision with
    // GA4's coarse built-in `platform` field which is `android|ios|web` only.
    const val PLATFORM = "kmp_platform"

    // Feature
    const val FEATURE_NAME = "feature_name"
    const val USAGE_COUNT = "usage_count"
    const val TUTORIAL_STEP = "tutorial_step"

    // Generic
    const val VALUE = "value"
    const val TIMESTAMP = "timestamp"
    const val DURATION = "duration"
    const val SUCCESS = "success"
}
