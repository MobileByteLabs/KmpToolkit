package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UriParserTest {

    @Test
    fun parse_fullUri_extractsAllComponents() {
        val link = UriParser.parse("myapp://open/product/42?ref=banner&tab=info#details")
        assertEquals("myapp", link.scheme)
        assertEquals("open", link.host)
        assertEquals("/product/42", link.path)
        assertEquals(listOf("product", "42"), link.pathSegments)
        assertEquals("banner", link.queryParams["ref"])
        assertEquals("info", link.queryParams["tab"])
        assertEquals("details", link.fragment)
    }

    @Test
    fun parse_noQueryOrFragment_returnsEmptyMaps() {
        val link = UriParser.parse("myapp://host/path")
        assertTrue(link.queryParams.isEmpty())
        assertNull(link.fragment)
    }

    @Test
    fun parse_httpsUrl_works() {
        val link = UriParser.parse("https://example.com/user/rajan?lang=en")
        assertEquals("https", link.scheme)
        assertEquals("example.com", link.host)
        assertEquals(listOf("user", "rajan"), link.pathSegments)
        assertEquals("en", link.queryParams["lang"])
    }

    @Test
    fun parse_encodedPathParam_decoded() {
        val link = UriParser.parse("myapp://open/search/hello%20world")
        assertEquals("hello world", link.pathSegments.last())
    }

    @Test
    fun parse_encodedQueryValue_decoded() {
        val link = UriParser.parse("myapp://open/search?q=hello%20world")
        assertEquals("hello world", link.queryParams["q"])
    }

    @Test
    fun parse_emptyPath_returnsEmptySegments() {
        val link = UriParser.parse("myapp://host")
        assertTrue(link.pathSegments.isEmpty())
        assertEquals("", link.path)
    }

    @Test
    fun parse_blankUri_returnsEmptyLink() {
        val link = UriParser.parse("")
        assertEquals("", link.scheme)
        assertEquals("", link.host)
        assertTrue(link.pathSegments.isEmpty())
    }

    @Test
    fun parse_malformedUri_doesNotThrow() {
        val link = UriParser.parse("not a uri at all!!!")
        assertEquals("not a uri at all!!!", link.raw)
    }

    @Test
    fun parse_noScheme_hostIsEmpty() {
        val link = UriParser.parse("/relative/path?key=val")
        assertEquals("", link.scheme)
    }

    @Test
    fun encodeComponent_specialChars_encoded() {
        assertEquals("hello%20world", UriParser.encodeComponent("hello world"))
        assertEquals("a%2Fb", UriParser.encodeComponent("a/b"))
    }

    @Test
    fun decodeComponent_encodedString_decoded() {
        assertEquals("hello world", UriParser.decodeComponent("hello%20world"))
        assertEquals("a/b", UriParser.decodeComponent("a%2Fb"))
    }

    @Test
    fun decodeComponent_malformedPercent_returnedUnchanged() {
        assertEquals("bad%ZZvalue", UriParser.decodeComponent("bad%ZZvalue"))
    }
}
