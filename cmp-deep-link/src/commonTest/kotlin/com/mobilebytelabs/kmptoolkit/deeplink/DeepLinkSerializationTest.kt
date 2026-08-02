package com.mobilebytelabs.kmptoolkit.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.serialization.json.Json

/**
 * Guards the kotlin.time.Instant migration (kotlinx-datetime 0.8.0, issue #143):
 * DeepLink.timestamp must serialize as an ISO-8601 string via [InstantIso8601Serializer],
 * matching the wire format the previous kotlinx.datetime.Instant serializer used so persisted
 * JSON stays compatible.
 */
class DeepLinkSerializationTest {

    private val sample = DeepLink(
        raw = "myapp://open/product/42?ref=home",
        scheme = "myapp",
        host = "open",
        path = "/product/42",
        pathSegments = listOf("product", "42"),
        queryParams = mapOf("ref" to "home"),
        fragment = null,
        timestamp = Instant.parse("2026-01-02T03:04:05Z"),
    )

    @Test
    fun roundTripsPreservingTimestamp() {
        val json = Json.encodeToString(DeepLink.serializer(), sample)
        val restored = Json.decodeFromString(DeepLink.serializer(), json)
        assertEquals(sample, restored)
        assertEquals(sample.timestamp, restored.timestamp)
    }

    @Test
    fun timestampIsEncodedAsIso8601String() {
        val json = Json.encodeToString(DeepLink.serializer(), sample)
        assertTrue(
            json.contains("\"2026-01-02T03:04:05Z\""),
            "expected ISO-8601 timestamp string in JSON, got: $json",
        )
    }
}
