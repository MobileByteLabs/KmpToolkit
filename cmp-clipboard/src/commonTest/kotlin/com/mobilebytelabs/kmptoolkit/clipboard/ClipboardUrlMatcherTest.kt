package com.mobilebytelabs.kmptoolkit.clipboard

import com.mobilebytelabs.kmptoolkit.clipboard.monitor.SocialMediaUrlMatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardUrlMatcherTest {

    // ── Instagram ───────────────────────────────────────────────────

    @Test
    fun instagram_matchesPostUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertTrue(matcher.matches("https://www.instagram.com/p/ABC123/"))
    }

    @Test
    fun instagram_matchesReelUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertTrue(matcher.matches("https://www.instagram.com/reel/ABC123/"))
    }

    @Test
    fun instagram_matchesReelsUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertTrue(matcher.matches("https://www.instagram.com/reels/ABC123/"))
    }

    @Test
    fun instagram_matchesStoryUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertTrue(matcher.matches("https://www.instagram.com/stories/user123/"))
    }

    @Test
    fun instagram_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertTrue(matcher.matches("https://instagr.am/p/ABC123"))
    }

    @Test
    fun instagram_extractsUrl() {
        val matcher = SocialMediaUrlMatchers.instagram()
        val url = matcher.extractUrl("Check this out: https://www.instagram.com/reel/ABC123/ amazing!")
        assertNotNull(url)
        assertTrue(url.contains("instagram.com/reel/ABC123"))
    }

    @Test
    fun instagram_doesNotMatchUnrelated() {
        val matcher = SocialMediaUrlMatchers.instagram()
        assertFalse(matcher.matches("https://www.google.com"))
    }

    // ── TikTok ──────────────────────────────────────────────────────

    @Test
    fun tiktok_matchesVideoUrl() {
        val matcher = SocialMediaUrlMatchers.tiktok()
        assertTrue(matcher.matches("https://www.tiktok.com/@user/video/1234567890"))
    }

    @Test
    fun tiktok_matchesVmShortUrl() {
        val matcher = SocialMediaUrlMatchers.tiktok()
        assertTrue(matcher.matches("https://vm.tiktok.com/ZMF2abc/"))
    }

    @Test
    fun tiktok_matchesVtShortUrl() {
        val matcher = SocialMediaUrlMatchers.tiktok()
        assertTrue(matcher.matches("https://vt.tiktok.com/ZSF2abc/"))
    }

    @Test
    fun tiktok_matchesTUrl() {
        val matcher = SocialMediaUrlMatchers.tiktok()
        assertTrue(matcher.matches("https://www.tiktok.com/t/ZTF2abc/"))
    }

    @Test
    fun tiktok_extractsUrl() {
        val matcher = SocialMediaUrlMatchers.tiktok()
        val url = matcher.extractUrl("Look at this: https://vm.tiktok.com/ZMF2abc/ so funny")
        assertNotNull(url)
        assertTrue(url.contains("vm.tiktok.com"))
    }

    // ── YouTube ─────────────────────────────────────────────────────

    @Test
    fun youtube_matchesWatchUrl() {
        val matcher = SocialMediaUrlMatchers.youtube()
        assertTrue(matcher.matches("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
    }

    @Test
    fun youtube_matchesShortsUrl() {
        val matcher = SocialMediaUrlMatchers.youtube()
        assertTrue(matcher.matches("https://www.youtube.com/shorts/ABC123"))
    }

    @Test
    fun youtube_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.youtube()
        assertTrue(matcher.matches("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun youtube_matchesPlaylistUrl() {
        val matcher = SocialMediaUrlMatchers.youtube()
        assertTrue(matcher.matches("https://www.youtube.com/playlist?list=PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"))
    }

    @Test
    fun youtube_extractsUrl() {
        val matcher = SocialMediaUrlMatchers.youtube()
        val url = matcher.extractUrl("Watch this: https://youtu.be/dQw4w9WgXcQ best video")
        assertNotNull(url)
        assertTrue(url.contains("youtu.be"))
    }

    // ── Twitter/X ───────────────────────────────────────────────────

    @Test
    fun twitter_matchesStatusUrl() {
        val matcher = SocialMediaUrlMatchers.twitter()
        assertTrue(matcher.matches("https://twitter.com/user/status/1234567890"))
    }

    @Test
    fun twitter_matchesXUrl() {
        val matcher = SocialMediaUrlMatchers.twitter()
        assertTrue(matcher.matches("https://x.com/user/status/1234567890"))
    }

    @Test
    fun twitter_matchesTcoUrl() {
        val matcher = SocialMediaUrlMatchers.twitter()
        assertTrue(matcher.matches("https://t.co/abc123"))
    }

    // ── Facebook ────────────────────────────────────────────────────

    @Test
    fun facebook_matchesVideoUrl() {
        val matcher = SocialMediaUrlMatchers.facebook()
        assertTrue(matcher.matches("https://www.facebook.com/user/videos/1234567890"))
    }

    @Test
    fun facebook_matchesWatchUrl() {
        val matcher = SocialMediaUrlMatchers.facebook()
        assertTrue(matcher.matches("https://www.facebook.com/watch/123456"))
    }

    @Test
    fun facebook_matchesReelUrl() {
        val matcher = SocialMediaUrlMatchers.facebook()
        assertTrue(matcher.matches("https://www.facebook.com/reel/123456"))
    }

    @Test
    fun facebook_matchesFbWatchUrl() {
        val matcher = SocialMediaUrlMatchers.facebook()
        assertTrue(matcher.matches("https://fb.watch/abc123/"))
    }

    // ── Snapchat ────────────────────────────────────────────────────

    @Test
    fun snapchat_matchesSpotlightUrl() {
        val matcher = SocialMediaUrlMatchers.snapchat()
        assertTrue(matcher.matches("https://www.snapchat.com/spotlight/abc123"))
    }

    @Test
    fun snapchat_matchesAddUrl() {
        val matcher = SocialMediaUrlMatchers.snapchat()
        assertTrue(matcher.matches("https://www.snapchat.com/add/username"))
    }

    @Test
    fun snapchat_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.snapchat()
        assertTrue(matcher.matches("https://t.snapchat.com/abc123"))
    }

    // ── Pinterest ───────────────────────────────────────────────────

    @Test
    fun pinterest_matchesPinUrl() {
        val matcher = SocialMediaUrlMatchers.pinterest()
        assertTrue(matcher.matches("https://www.pinterest.com/pin/123456789/"))
    }

    @Test
    fun pinterest_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.pinterest()
        assertTrue(matcher.matches("https://pin.it/abc123"))
    }

    // ── Reddit ──────────────────────────────────────────────────────

    @Test
    fun reddit_matchesPostUrl() {
        val matcher = SocialMediaUrlMatchers.reddit()
        assertTrue(matcher.matches("https://www.reddit.com/r/kotlin/comments/abc123/my_post"))
    }

    @Test
    fun reddit_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.reddit()
        assertTrue(matcher.matches("https://redd.it/abc123"))
    }

    // ── LinkedIn ────────────────────────────────────────────────────

    @Test
    fun linkedin_matchesPostUrl() {
        val matcher = SocialMediaUrlMatchers.linkedin()
        assertTrue(matcher.matches("https://www.linkedin.com/posts/user-activity-123"))
    }

    @Test
    fun linkedin_matchesFeedUrl() {
        val matcher = SocialMediaUrlMatchers.linkedin()
        assertTrue(matcher.matches("https://www.linkedin.com/feed/update/urn:li:activity:123"))
    }

    // ── Threads ─────────────────────────────────────────────────────

    @Test
    fun threads_matchesPostUrl() {
        val matcher = SocialMediaUrlMatchers.threads()
        assertTrue(matcher.matches("https://www.threads.net/@user/post/ABC123"))
    }

    @Test
    fun threads_matchesShortUrl() {
        val matcher = SocialMediaUrlMatchers.threads()
        assertTrue(matcher.matches("https://www.threads.net/t/ABC123"))
    }

    // ── All Matchers ────────────────────────────────────────────────

    @Test
    fun allMatchers_returns10() {
        val all = SocialMediaUrlMatchers.all()
        assertEquals(10, all.size)
    }

    @Test
    fun allMatchers_haveUniqueNames() {
        val all = SocialMediaUrlMatchers.all()
        val names = all.map { it.name }.toSet()
        assertEquals(all.size, names.size)
    }

    @Test
    fun noMatcher_matchesPlainText() {
        val all = SocialMediaUrlMatchers.all()
        val plainText = "This is just regular text with no URLs"
        assertFalse(all.any { it.matches(plainText) })
    }

    @Test
    fun noMatcher_extractsFromPlainText() {
        val all = SocialMediaUrlMatchers.all()
        val plainText = "This is just regular text"
        all.forEach { matcher ->
            assertNull(matcher.extractUrl(plainText))
        }
    }

    @Test
    fun matcherDetection_withEmbeddedUrl() {
        val all = SocialMediaUrlMatchers.all()
        val text = "Hey check this video https://www.tiktok.com/@user/video/1234567890 it's amazing!"
        val matched = all.filter { it.matches(text) }
        assertEquals(1, matched.size)
        assertEquals("TikTok", matched.first().name)
    }

    @Test
    fun matcherDetection_withMultipleUrls() {
        val all = SocialMediaUrlMatchers.all()
        // Instagram URL mixed with other text
        val text = "https://www.instagram.com/reel/ABC123/"
        val matched = all.filter { it.matches(text) }
        assertTrue(matched.any { it.name == "Instagram" })
    }
}
