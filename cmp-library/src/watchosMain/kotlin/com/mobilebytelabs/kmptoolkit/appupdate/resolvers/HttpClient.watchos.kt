package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * watchOS implementation of HTTP client.
 * Not used since watchOS doesn't support in-app updates.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String =
        throw UnsupportedOperationException("HTTP client not needed on watchOS")
}
