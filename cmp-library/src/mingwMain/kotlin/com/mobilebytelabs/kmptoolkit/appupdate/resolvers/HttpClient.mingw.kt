package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * Windows (mingw) implementation of HTTP client.
 * Windows native doesn't have built-in HTTP client in this implementation.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String = throw UnsupportedOperationException(
        "HTTP client not available on Windows native. " +
            "Use JVM target for desktop applications with full HTTP support.",
    )
}
