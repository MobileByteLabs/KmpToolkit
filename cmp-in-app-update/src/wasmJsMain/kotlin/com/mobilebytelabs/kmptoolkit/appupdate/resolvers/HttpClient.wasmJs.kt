package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * WasmJs implementation of HTTP client.
 * Not used since WasmJs doesn't support in-app updates.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String =
        throw UnsupportedOperationException("HTTP client not needed on WasmJs")
}
