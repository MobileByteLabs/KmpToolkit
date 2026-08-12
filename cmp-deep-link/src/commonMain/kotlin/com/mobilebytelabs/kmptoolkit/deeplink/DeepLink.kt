package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Represents a parsed deep link URI.
 *
 * Created by [DeepLinkHandler.handle] when a URI is received from any platform.
 * Immutable — create a new instance for each link received.
 *
 * @param raw The original URI string as received from the OS.
 * @param scheme URI scheme (e.g. `myapp`, `https`).
 * @param host URI host / authority (e.g. `open`, `example.com`).
 * @param path Full path component (e.g. `/product/42`).
 * @param pathSegments Individual path segments split by `/` (empty segments removed).
 * @param queryParams Query string parsed into key→value pairs.
 * @param fragment Fragment component after `#`, or `null` if absent.
 * @param timestamp When this link was received; defaults to [Clock.System.now].
 */
@Serializable
data class DeepLink(
    val raw: String,
    val scheme: String,
    val host: String,
    val path: String,
    val pathSegments: List<String>,
    val queryParams: Map<String, String>,
    val fragment: String?,
    val timestamp: Instant = Clock.System.now(),
) {
    companion object {
        /**
         * Parse a URI string into a [DeepLink] without going through [DeepLinkHandler].
         *
         * Useful for one-shot parsing in tests and demos where you already have the URI
         * string and don't need to emit it through the global handler.
         */
        fun parse(uri: String): DeepLink = UriParser.parse(uri)
    }
}
