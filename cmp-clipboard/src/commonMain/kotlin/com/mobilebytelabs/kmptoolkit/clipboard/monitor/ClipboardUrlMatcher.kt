package com.mobilebytelabs.kmptoolkit.clipboard.monitor

/**
 * Matches clipboard content against URL patterns for a specific platform/service.
 *
 * Implement this interface to detect URLs from specific social media or content
 * platforms. Register matchers with [ClipboardMonitor.addUrlMatcher][com.mobilebytelabs.kmptoolkit.clipboard.ClipboardMonitor.addUrlMatcher].
 *
 * ## Usage
 *
 * ```kotlin
 * val monitor = createClipboardMonitor()
 *
 * // Use built-in matchers
 * SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
 *
 * // Or create custom matchers
 * val customMatcher = object : ClipboardUrlMatcher {
 *     override val name = "MyService"
 *     override val patterns = listOf(Regex("https?://myservice\\.com/.+"))
 *     override fun matches(content: String) = patterns.any { it.containsMatchIn(content) }
 *     override fun extractUrl(content: String) = patterns.firstNotNullOfOrNull {
 *         it.find(content)?.value
 *     }
 * }
 * monitor.addUrlMatcher(customMatcher)
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardUrlMatcher {
    /** Human-readable name of the platform (e.g., "Instagram", "TikTok"). */
    val name: String

    /** Regex patterns to match URLs from this platform. */
    val patterns: List<Regex>

    /** Returns true if the content contains a URL matching this platform. */
    fun matches(content: String): Boolean

    /** Extracts the first matching URL from the content, or null if no match. */
    fun extractUrl(content: String): String?
}

/**
 * Built-in URL matchers for popular social media and content platforms.
 *
 * @since 0.2.0
 */
object SocialMediaUrlMatchers {

    /** Matches Instagram post, reel, story, and profile URLs. */
    fun instagram(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Instagram",
        patterns = listOf(
            Regex("https?://(?:www\\.)?instagram\\.com/(?:p|reel|reels|stories|tv)/[\\w.-]+/?[^\\s]*"),
            Regex("https?://(?:www\\.)?instagram\\.com/[\\w.-]+/?(?:\\?[^\\s]*)?"),
            Regex("https?://(?:www\\.)?instagr\\.am/[\\w.-]+/?[^\\s]*"),
        ),
    )

    /** Matches TikTok video and profile URLs. */
    fun tiktok(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "TikTok",
        patterns = listOf(
            Regex("https?://(?:www\\.)?tiktok\\.com/@[\\w.-]+/video/\\d+[^\\s]*"),
            Regex("https?://(?:vm|vt)\\.tiktok\\.com/[\\w]+/?[^\\s]*"),
            Regex("https?://(?:www\\.)?tiktok\\.com/t/[\\w]+/?[^\\s]*"),
        ),
    )

    /** Matches YouTube video, shorts, and playlist URLs. */
    fun youtube(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "YouTube",
        patterns = listOf(
            Regex("https?://(?:www\\.)?youtube\\.com/watch\\?v=[\\w-]+[^\\s]*"),
            Regex("https?://(?:www\\.)?youtube\\.com/shorts/[\\w-]+[^\\s]*"),
            Regex("https?://youtu\\.be/[\\w-]+[^\\s]*"),
            Regex("https?://(?:www\\.)?youtube\\.com/playlist\\?list=[\\w-]+[^\\s]*"),
        ),
    )

    /** Matches Twitter/X post URLs. */
    fun twitter(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Twitter",
        patterns = listOf(
            Regex("https?://(?:www\\.)?(?:twitter|x)\\.com/[\\w]+/status/\\d+[^\\s]*"),
            Regex("https?://t\\.co/[\\w]+[^\\s]*"),
        ),
    )

    /** Matches Facebook video and post URLs. */
    fun facebook(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Facebook",
        patterns = listOf(
            Regex("https?://(?:www\\.)?facebook\\.com/.+/videos/\\d+[^\\s]*"),
            Regex("https?://(?:www\\.)?facebook\\.com/(?:watch|reel)/[^\\s]+"),
            Regex("https?://(?:www\\.)?facebook\\.com/[\\w.]+/posts/[^\\s]+"),
            Regex("https?://fb\\.watch/[\\w]+[^\\s]*"),
        ),
    )

    /** Matches Snapchat spotlight and story URLs. */
    fun snapchat(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Snapchat",
        patterns = listOf(
            Regex("https?://(?:www\\.)?snapchat\\.com/spotlight/[^\\s]+"),
            Regex("https?://(?:www\\.)?snapchat\\.com/add/[\\w.-]+[^\\s]*"),
            Regex("https?://t\\.snapchat\\.com/[\\w]+[^\\s]*"),
        ),
    )

    /** Matches Pinterest pin URLs. */
    fun pinterest(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Pinterest",
        patterns = listOf(
            Regex("https?://(?:www\\.)?pinterest\\.com/pin/[\\d]+[^\\s]*"),
            Regex("https?://pin\\.it/[\\w]+[^\\s]*"),
        ),
    )

    /** Matches Reddit post URLs. */
    fun reddit(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Reddit",
        patterns = listOf(
            Regex("https?://(?:www\\.)?reddit\\.com/r/[\\w]+/comments/[\\w]+[^\\s]*"),
            Regex("https?://redd\\.it/[\\w]+[^\\s]*"),
        ),
    )

    /** Matches LinkedIn post URLs. */
    fun linkedin(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "LinkedIn",
        patterns = listOf(
            Regex("https?://(?:www\\.)?linkedin\\.com/posts/[^\\s]+"),
            Regex("https?://(?:www\\.)?linkedin\\.com/feed/update/[^\\s]+"),
        ),
    )

    /** Matches Threads post URLs. */
    fun threads(): ClipboardUrlMatcher = RegexUrlMatcher(
        name = "Threads",
        patterns = listOf(
            Regex("https?://(?:www\\.)?threads\\.net/@[\\w.-]+/post/[\\w]+[^\\s]*"),
            Regex("https?://(?:www\\.)?threads\\.net/t/[\\w]+[^\\s]*"),
        ),
    )

    /** Returns all built-in social media URL matchers. */
    fun all(): List<ClipboardUrlMatcher> = listOf(
        instagram(),
        tiktok(),
        youtube(),
        twitter(),
        facebook(),
        snapchat(),
        pinterest(),
        reddit(),
        linkedin(),
        threads(),
    )
}

/**
 * Default [ClipboardUrlMatcher] implementation using regex patterns.
 */
internal class RegexUrlMatcher(override val name: String, override val patterns: List<Regex>) : ClipboardUrlMatcher {

    override fun matches(content: String): Boolean = patterns.any { it.containsMatchIn(content) }

    override fun extractUrl(content: String): String? = patterns.firstNotNullOfOrNull { regex ->
        regex.find(content)?.value
    }
}
