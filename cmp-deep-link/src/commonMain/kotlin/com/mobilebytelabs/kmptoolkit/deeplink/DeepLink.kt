package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
    @Serializable(with = InstantIso8601Serializer::class)
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

/**
 * Serializes a [kotlin.time.Instant] as an ISO-8601 string.
 *
 * kotlinx-datetime 0.8.0 migrated to `kotlin.time.Instant` and dropped its standalone Instant
 * ISO serializer object — serialization of `kotlin.time.Instant` is now owned by
 * kotlinx-serialization 1.9.0+. This repo is on kotlinx-serialization 1.8.1, so we provide a
 * minimal ISO serializer locally. It emits the same wire format the previous
 * `kotlinx.datetime.Instant` serializer used, so persisted JSON stays compatible. Remove once the
 * repo adopts kotlinx-serialization ≥ 1.9.0 (the built-in serializer is then found automatically).
 */
internal object InstantIso8601Serializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("kotlin.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
