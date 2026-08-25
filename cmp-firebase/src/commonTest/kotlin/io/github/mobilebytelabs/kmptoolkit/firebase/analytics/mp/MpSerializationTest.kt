/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the GA4 Measurement-Protocol wire contract: the payload MUST use snake_case
 * top-level keys (`client_id` / `user_id` / `user_properties`). With camelCase, GA4
 * returns 2xx but silently DROPS the event (no valid `client_id`) — which would defeat
 * the whole MP tier (non-GitLive analytics + the fallback-tier crash→GA4 mirror).
 */
class MpSerializationTest {

    @Test
    fun payload_uses_ga4_snake_case_keys() {
        val json = Json.encodeToString(
            MpRequest(
                clientId = "cid-123",
                userId = "uid-9",
                userProperties = mapOf("tier" to UserPropertyValue("pro")),
                events = listOf(MpEvent(name = "app_crash", params = mapOf("kmp_platform" to JsonPrimitive("jvm")))),
            ),
        )
        assertTrue("client_id" in json, "payload must contain snake_case client_id: $json")
        assertTrue("user_id" in json, "payload must contain snake_case user_id: $json")
        assertTrue("user_properties" in json, "payload must contain snake_case user_properties: $json")
        // The camelCase forms would make GA4 drop the event — must NOT appear.
        assertFalse("clientId" in json, "payload must NOT contain camelCase clientId: $json")
        assertFalse("userProperties" in json, "payload must NOT contain camelCase userProperties: $json")
    }
}
