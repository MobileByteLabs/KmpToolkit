package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.RoutePattern
import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser

/**
 * Constructs deep link URIs in the mirror image of [DeepLinkParser].
 *
 * ## Example
 *
 * ```kotlin
 * val builder = DeepLinkBuilder(scheme = "myapp", host = "open")
 *
 * // Simple path + query
 * val uri = builder.build("/product/{id}", mapOf("id" to "42"))
 * // → "myapp://open/product/42"
 *
 * // With query params
 * val uri2 = builder.build("/search", queryParams = mapOf("q" to "hello world"))
 * // → "myapp://open/search?q=hello%20world"
 * ```
 *
 * @param scheme URI scheme without `://` (e.g. `myapp`, `https`).
 * @param host URI host / authority (e.g. `open`, `example.com`).
 */
class DeepLinkBuilder(private val scheme: String, private val host: String) {

    /**
     * Build a URI string for the given [pathPattern] and parameter maps.
     *
     * @param pathPattern Path with optional `{param}` placeholders (same syntax as [DeepLinkParser]).
     * @param pathParams Values for `{param}` placeholders in [pathPattern].
     * @param queryParams Additional key=value pairs appended as a query string.
     * @return Fully-formed URI string.
     */
    fun build(
        pathPattern: String,
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
    ): String {
        val path = RoutePattern(pathPattern).build(pathParams)
        return buildString {
            append(scheme)
            append("://")
            append(host)
            append(path)
            if (queryParams.isNotEmpty()) {
                append('?')
                queryParams.entries.forEachIndexed { idx, (key, value) ->
                    if (idx > 0) append('&')
                    append(UriParser.encodeComponent(key))
                    append('=')
                    append(UriParser.encodeComponent(value))
                }
            }
        }
    }
}
