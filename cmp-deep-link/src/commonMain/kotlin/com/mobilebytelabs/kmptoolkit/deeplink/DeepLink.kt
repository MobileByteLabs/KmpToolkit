package com.mobilebytelabs.kmptoolkit.deeplink

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
)
