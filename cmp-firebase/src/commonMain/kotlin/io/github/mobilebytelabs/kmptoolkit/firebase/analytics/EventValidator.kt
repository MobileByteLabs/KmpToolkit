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
 * Validates analytics events for taxonomy compliance and PII risk.
 *
 * Use during development (assert at debug-build event log site) or as a build-time check
 * on auto-generated event classes. Production code should NOT validate at log time —
 * use [AnalyticsEvent]'s init-block validation instead, which is faster.
 *
 * @sample
 * ```kotlin
 * val validator = EventValidator()
 * val result = validator.validate(event)
 * if (result.errors.isNotEmpty()) {
 *     Logger.w { "Event validation: ${result.errors.joinToString()}" }
 * }
 * ```
 */
class EventValidator(
    private val taxonomyRegex: Regex = DEFAULT_TAXONOMY_REGEX,
    private val piiRegex: Regex = DEFAULT_PII_REGEX,
) {

    data class Result(
        val event: AnalyticsEvent,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList(),
    ) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    fun validate(event: AnalyticsEvent): Result {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // Event-name taxonomy: snake_case, 2-40 chars, no leading digit
        if (!taxonomyRegex.matches(event.type)) {
            errors.add(
                "Event type '${event.type}' violates taxonomy: must be snake_case, " +
                    "2-40 chars, no leading digit, no special chars.",
            )
        }

        // Param-key taxonomy
        for (extra in event.extras) {
            if (!taxonomyRegex.matches(extra.key)) {
                errors.add("Param key '${extra.key}' violates taxonomy.")
            }

            // PII regex check on values
            if (piiRegex.containsMatchIn(extra.value)) {
                errors.add(
                    "Param '${extra.key}' value matches PII pattern. " +
                        "Strip PII before logging — use opaque IDs instead.",
                )
            }
        }

        // Suggest standard keys when custom keys closely match a standard one
        for (extra in event.extras) {
            val suggestion = STANDARD_KEY_SUGGESTIONS[extra.key.lowercase()]
            if (suggestion != null && suggestion != extra.key) {
                warnings.add("Param key '${extra.key}' — consider standard '$suggestion'.")
            }
        }

        return Result(event, errors, warnings)
    }

    companion object {
        /** snake_case, 2-40 chars, starts with a letter, no special chars. */
        val DEFAULT_TAXONOMY_REGEX = Regex("^[a-z][a-z0-9_]{1,39}$")

        /** Common PII patterns: email, phone (E.164-ish), credit card (Luhn-ignored). */
        val DEFAULT_PII_REGEX = Regex(
            """([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})""" + // email
                """|(\+?\d{1,3}[\s-]?\(?\d{1,4}\)?[\s-]?\d{3,5}[\s-]?\d{3,5})""" + // phone
                """|(\b\d{12,19}\b)""" + // 12-19 digit pan
                """|(\b\d{3}-\d{2}-\d{4}\b)""", // SSN
        )

        /** Lowercased common typos → standard ParamKeys. */
        val STANDARD_KEY_SUGGESTIONS = mapOf(
            "screen" to ParamKeys.SCREEN_NAME,
            "screenname" to ParamKeys.SCREEN_NAME,
            "button" to ParamKeys.BUTTON_NAME,
            "buttonname" to ParamKeys.BUTTON_NAME,
            "userid" to ParamKeys.USER_ID,
            "errorcode" to ParamKeys.ERROR_CODE,
            "errormsg" to ParamKeys.ERROR_MESSAGE,
        )
    }
}
