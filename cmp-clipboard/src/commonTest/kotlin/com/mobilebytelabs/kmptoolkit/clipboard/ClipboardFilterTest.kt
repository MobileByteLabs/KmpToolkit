package com.mobilebytelabs.kmptoolkit.clipboard

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClipboardFilterTest {

    // ── urlOnly Filter ──────────────────────────────────────────────

    @Test
    fun urlOnly_acceptsHttps() {
        val filter = ClipboardFilter.urlOnly()
        assertTrue(filter.shouldProcess("https://example.com"))
    }

    @Test
    fun urlOnly_acceptsHttp() {
        val filter = ClipboardFilter.urlOnly()
        assertTrue(filter.shouldProcess("http://example.com"))
    }

    @Test
    fun urlOnly_rejectsPlainText() {
        val filter = ClipboardFilter.urlOnly()
        assertFalse(filter.shouldProcess("just some text"))
    }

    @Test
    fun urlOnly_rejectsEmptyString() {
        val filter = ClipboardFilter.urlOnly()
        assertFalse(filter.shouldProcess(""))
    }

    @Test
    fun urlOnly_rejectsFtpUrl() {
        val filter = ClipboardFilter.urlOnly()
        assertFalse(filter.shouldProcess("ftp://files.example.com"))
    }

    // ── minLength Filter ────────────────────────────────────────────

    @Test
    fun minLength_acceptsLongEnough() {
        val filter = ClipboardFilter.minLength(5)
        assertTrue(filter.shouldProcess("hello world"))
    }

    @Test
    fun minLength_acceptsExactLength() {
        val filter = ClipboardFilter.minLength(5)
        assertTrue(filter.shouldProcess("hello"))
    }

    @Test
    fun minLength_rejectsTooShort() {
        val filter = ClipboardFilter.minLength(10)
        assertFalse(filter.shouldProcess("short"))
    }

    @Test
    fun minLength_rejectsEmpty() {
        val filter = ClipboardFilter.minLength(1)
        assertFalse(filter.shouldProcess(""))
    }

    // ── maxLength Filter ────────────────────────────────────────────

    @Test
    fun maxLength_acceptsShortEnough() {
        val filter = ClipboardFilter.maxLength(100)
        assertTrue(filter.shouldProcess("short text"))
    }

    @Test
    fun maxLength_acceptsExactLength() {
        val filter = ClipboardFilter.maxLength(5)
        assertTrue(filter.shouldProcess("hello"))
    }

    @Test
    fun maxLength_rejectsTooLong() {
        val filter = ClipboardFilter.maxLength(5)
        assertFalse(filter.shouldProcess("this is way too long"))
    }

    @Test
    fun maxLength_acceptsEmpty() {
        val filter = ClipboardFilter.maxLength(100)
        assertTrue(filter.shouldProcess(""))
    }

    // ── exclude Filter ──────────────────────────────────────────────

    @Test
    fun exclude_rejectsMatchingPattern() {
        val filter = ClipboardFilter.exclude(Regex("password", RegexOption.IGNORE_CASE))
        assertFalse(filter.shouldProcess("my password is 1234"))
    }

    @Test
    fun exclude_acceptsNonMatchingContent() {
        val filter = ClipboardFilter.exclude(Regex("password", RegexOption.IGNORE_CASE))
        assertTrue(filter.shouldProcess("https://instagram.com/reel/123"))
    }

    @Test
    fun exclude_multiplePatterns() {
        val filter = ClipboardFilter.exclude(
            Regex("password", RegexOption.IGNORE_CASE),
            Regex("secret", RegexOption.IGNORE_CASE),
            Regex("token", RegexOption.IGNORE_CASE)
        )
        assertFalse(filter.shouldProcess("my secret key"))
        assertFalse(filter.shouldProcess("bearer token abc"))
        assertTrue(filter.shouldProcess("https://youtube.com/watch?v=abc"))
    }

    // ── Filter Names ────────────────────────────────────────────────

    @Test
    fun filters_haveDescriptiveNames() {
        assertTrue(ClipboardFilter.urlOnly().name.isNotEmpty())
        assertTrue(ClipboardFilter.minLength(5).name.contains("5"))
        assertTrue(ClipboardFilter.maxLength(100).name.contains("100"))
        assertTrue(ClipboardFilter.exclude(Regex("test")).name.contains("1"))
    }

    // ── Custom Filter ───────────────────────────────────────────────

    @Test
    fun customFilter_works() {
        val noNumbers = object : ClipboardFilter {
            override val name = "NoNumbers"
            override fun shouldProcess(content: String) = !content.any { it.isDigit() }
        }
        assertTrue(noNumbers.shouldProcess("hello world"))
        assertFalse(noNumbers.shouldProcess("hello 123"))
    }

    // ── Combined Filters ────────────────────────────────────────────

    @Test
    fun combinedFilters_allMustPass() {
        val filters = listOf(
            ClipboardFilter.urlOnly(),
            ClipboardFilter.minLength(20),
            ClipboardFilter.maxLength(500)
        )

        val url = "https://www.instagram.com/reel/ABC123/"
        assertTrue(filters.all { it.shouldProcess(url) })

        val shortUrl = "https://t.co/a"
        assertFalse(filters.all { it.shouldProcess(shortUrl) }) // fails minLength

        val plainText = "just some text that is long enough"
        assertFalse(filters.all { it.shouldProcess(plainText) }) // fails urlOnly
    }
}
