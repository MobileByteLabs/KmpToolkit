package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * Linux implementation of HTTP client.
 * Linux native doesn't have built-in HTTP client.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String = throw UnsupportedOperationException(
        "HTTP client not available on Linux native. " +
            "Use JVM target for desktop applications with full HTTP support.",
    )
}
