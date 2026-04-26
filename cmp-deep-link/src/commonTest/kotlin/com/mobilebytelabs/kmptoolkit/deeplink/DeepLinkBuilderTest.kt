package com.mobilebytelabs.kmptoolkit.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeepLinkBuilderTest {

    private val builder = DeepLinkBuilder(scheme = "myapp", host = "open")

    @Test
    fun build_simplePath_noParams() {
        assertEquals("myapp://open/about", builder.build("/about"))
    }

    @Test
    fun build_pathParam_inserted() {
        assertEquals(
            "myapp://open/product/42",
            builder.build("/product/{id}", pathParams = mapOf("id" to "42")),
        )
    }

    @Test
    fun build_queryParams_appended() {
        val uri = builder.build("/search", queryParams = mapOf("q" to "hello world", "lang" to "en"))
        assertTrue(uri.startsWith("myapp://open/search?"))
        assertTrue(uri.contains("q=hello%20world"))
        assertTrue(uri.contains("lang=en"))
    }

    @Test
    fun build_pathAndQueryParams_combined() {
        val uri = builder.build(
            "/user/{username}/posts",
            pathParams = mapOf("username" to "rajan"),
            queryParams = mapOf("tab" to "videos"),
        )
        assertEquals("myapp://open/user/rajan/posts?tab=videos", uri)
    }

    @Test
    fun build_pathParamSpecialChars_encoded() {
        val uri = builder.build("/search/{id}", pathParams = mapOf("id" to "hello world"))
        assertEquals("myapp://open/search/hello%20world", uri)
    }

    @Test
    fun build_roundTrip_parsedBack() {
        val uri = builder.build("/product/{id}", pathParams = mapOf("id" to "99"))
        val parser = deepLinkParser {
            route<TestProductRoute>("/product/{id}")
        }
        val parsed = parser.parse<TestProductRoute>(
            com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser.parse(uri),
        )
        assertEquals("99", parsed?.id)
    }
}

@kotlinx.serialization.Serializable
private data class TestProductRoute(val id: String)
