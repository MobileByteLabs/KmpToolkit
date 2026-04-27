package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Serializable
private data class ProductRoute(val id: String)

@Serializable
private data class ProfileRoute(val username: String, val tab: String = "posts")

@Serializable
private data class SearchRoute(val query: String)

class DeepLinkParserTest {

    private val parser = deepLinkParser {
        route<ProductRoute>("/product/{id}")
        route<ProfileRoute>("/user/{username}/posts")
        route<SearchRoute>("/search/{*query}")
    }

    private fun link(path: String, query: Map<String, String> = emptyMap()) = DeepLink(
        raw = "myapp://host$path",
        scheme = "myapp",
        host = "host",
        path = path,
        pathSegments = path.split('/').filter { it.isNotEmpty() },
        queryParams = query,
        fragment = null,
    )

    @Test
    fun parse_matchingRoute_returnsTypedObject() {
        val result: ProductRoute? = parser.parse(link("/product/42"))
        assertNotNull(result)
        assertEquals("42", result.id)
    }

    @Test
    fun parse_profileRoute_extracted() {
        val result: ProfileRoute? = parser.parse(link("/user/rajan/posts"))
        assertNotNull(result)
        assertEquals("rajan", result.username)
    }

    @Test
    fun parse_queryParamOverridesDefault() {
        val result: ProfileRoute? = parser.parse(link("/user/rajan/posts", mapOf("tab" to "videos")))
        assertNotNull(result)
        assertEquals("videos", result.tab)
    }

    @Test
    fun parse_catchAllRoute_joinsSegments() {
        val result: SearchRoute? = parser.parse(link("/search/hello/world"))
        assertNotNull(result)
        assertEquals("hello/world", result.query)
    }

    @Test
    fun parse_noMatchingRoute_returnsNull() {
        val result: ProductRoute? = parser.parse(link("/unknown/path"))
        assertNull(result)
    }

    @Test
    fun parse_wrongTypeForRoute_returnsNull() {
        // Route /product/{id} matches but caller asks for ProfileRoute — mismatch
        val result: ProfileRoute? = parser.parse(link("/product/42"))
        assertNull(result)
    }

    @Test
    fun parse_fullUriRoundTrip() {
        val uri = "myapp://open/product/99?lang=en"
        val deepLink = UriParser.parse(uri)
        val result: ProductRoute? = parser.parse(deepLink)
        assertNotNull(result)
        assertEquals("99", result.id)
    }
}
