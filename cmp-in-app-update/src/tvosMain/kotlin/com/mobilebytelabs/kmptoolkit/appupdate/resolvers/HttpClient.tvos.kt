package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * tvOS implementation of HTTP client.
 * Not used since tvOS doesn't support in-app updates.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String =
        throw UnsupportedOperationException("HTTP client not needed on tvOS")
}
