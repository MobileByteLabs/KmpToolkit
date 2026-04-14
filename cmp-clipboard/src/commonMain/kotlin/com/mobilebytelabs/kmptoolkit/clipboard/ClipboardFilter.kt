package com.mobilebytelabs.kmptoolkit.clipboard

/**
 * Filters clipboard content to include or exclude specific patterns.
 *
 * Filters are applied before URL matching. Content that doesn't pass
 * any registered filter is silently dropped.
 *
 * ## Usage
 *
 * ```kotlin
 * val monitor = createClipboardMonitor()
 *
 * // Only process content that contains URLs
 * monitor.addFilter(ClipboardFilter.urlOnly())
 *
 * // Exclude short content (likely not URLs)
 * monitor.addFilter(ClipboardFilter.minLength(10))
 *
 * // Custom filter
 * monitor.addFilter(object : ClipboardFilter {
 *     override val name = "NoPasswords"
 *     override fun shouldProcess(content: String) =
 *         !content.contains("password", ignoreCase = true)
 * })
 * ```
 *
 * @since 0.2.0
 */
interface ClipboardFilter {
    /** Human-readable name of this filter. */
    val name: String

    /**
     * Returns true if the content should be processed, false to skip it.
     *
     * @param content The clipboard text content to evaluate.
     */
    fun shouldProcess(content: String): Boolean

    companion object {
        /** Only process content that looks like a URL. */
        fun urlOnly(): ClipboardFilter = object : ClipboardFilter {
            override val name = "UrlOnly"
            override fun shouldProcess(content: String) =
                content.startsWith("http://") || content.startsWith("https://")
        }

        /** Only process content with at least [length] characters. */
        fun minLength(length: Int): ClipboardFilter = object : ClipboardFilter {
            override val name = "MinLength($length)"
            override fun shouldProcess(content: String) = content.length >= length
        }

        /** Only process content with at most [length] characters. */
        fun maxLength(length: Int): ClipboardFilter = object : ClipboardFilter {
            override val name = "MaxLength($length)"
            override fun shouldProcess(content: String) = content.length <= length
        }

        /** Exclude content matching any of the given patterns. */
        fun exclude(vararg patterns: Regex): ClipboardFilter = object : ClipboardFilter {
            override val name = "Exclude(${patterns.size} patterns)"
            override fun shouldProcess(content: String) = patterns.none { it.containsMatchIn(content) }
        }
    }
}
