package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.RoutePattern
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoutePatternTest {

    @Test
    fun match_literalSegments_matches() {
        val pattern = RoutePattern("/product/detail")
        val result = pattern.match(listOf("product", "detail"))
        assertNotNull(result)
        assertEquals(emptyMap(), result)
    }

    @Test
    fun match_requiredParam_extracted() {
        val pattern = RoutePattern("/product/{id}")
        val result = pattern.match(listOf("product", "42"))
        assertNotNull(result)
        assertEquals("42", result["id"])
    }

    @Test
    fun match_multipleRequiredParams_allExtracted() {
        val pattern = RoutePattern("/user/{username}/posts/{postId}")
        val result = pattern.match(listOf("user", "rajan", "posts", "99"))
        assertNotNull(result)
        assertEquals("rajan", result["username"])
        assertEquals("99", result["postId"])
    }

    @Test
    fun match_optionalParam_presentSegment_extracted() {
        val pattern = RoutePattern("/item/{tab?}")
        val result = pattern.match(listOf("item", "reviews"))
        assertNotNull(result)
        assertEquals("reviews", result["tab"])
    }

    @Test
    fun match_optionalParam_absentSegment_matchesWithEmpty() {
        val pattern = RoutePattern("/item/{tab?}")
        val result = pattern.match(listOf("item"))
        assertNotNull(result)
        // Optional param absent → not in map (or empty string)
        assertEquals(true, result["tab"] == null || result["tab"] == "")
    }

    @Test
    fun match_catchAll_consumesRemaining() {
        val pattern = RoutePattern("/search/{*query}")
        val result = pattern.match(listOf("search", "hello", "world"))
        assertNotNull(result)
        assertEquals("hello/world", result["query"])
    }

    @Test
    fun match_catchAll_emptyRemaining_matchesWithEmpty() {
        val pattern = RoutePattern("/search/{*query}")
        val result = pattern.match(listOf("search"))
        assertNotNull(result)
        assertEquals("", result["query"])
    }

    @Test
    fun match_wrongLiteral_returnsNull() {
        val pattern = RoutePattern("/product/{id}")
        assertNull(pattern.match(listOf("order", "42")))
    }

    @Test
    fun match_tooFewSegments_returnsNull() {
        val pattern = RoutePattern("/product/{id}")
        assertNull(pattern.match(listOf("product")))
    }

    @Test
    fun match_tooManySegments_returnsNull() {
        val pattern = RoutePattern("/product/{id}")
        assertNull(pattern.match(listOf("product", "42", "extra")))
    }

    @Test
    fun build_requiredParam_inserted() {
        val pattern = RoutePattern("/product/{id}")
        assertEquals("/product/42", pattern.build(mapOf("id" to "42")))
    }

    @Test
    fun build_queryEncodesSpecialChars() {
        val pattern = RoutePattern("/search/{*query}")
        val path = pattern.build(mapOf("query" to "hello/world"))
        assertEquals("/search/hello/world", path)
    }
}
