package com.mobilebytelabs.kmptoolkit.deeplink.internal

import com.mobilebytelabs.kmptoolkit.deeplink.DeepLink

/**
 * Pure-Kotlin URI parser. Zero reflection. No platform APIs.
 * Handles malformed input gracefully — always returns a [DeepLink].
 */
internal object UriParser {

    /**
     * Parse [uri] into a [DeepLink]. Never throws; malformed fields default to empty strings.
     */
    fun parse(uri: String): DeepLink {
        if (uri.isBlank()) return empty(uri)

        // Split fragment first
        val (withoutFragment, fragment) = splitFragment(uri)

        // Split scheme
        val schemeEnd = withoutFragment.indexOf("://")
        val scheme: String
        val rest: String
        if (schemeEnd >= 0) {
            scheme = withoutFragment.substring(0, schemeEnd)
            rest = withoutFragment.substring(schemeEnd + 3)
        } else {
            scheme = ""
            rest = withoutFragment
        }

        // Split host from path
        val slashIdx = rest.indexOf('/')
        val host: String
        val rawPath: String
        if (slashIdx >= 0) {
            host = rest.substring(0, slashIdx)
            rawPath = rest.substring(slashIdx)
        } else {
            // might have query on host
            val qIdx = rest.indexOf('?')
            if (qIdx >= 0) {
                host = rest.substring(0, qIdx)
                rawPath = ""
            } else {
                host = rest
                rawPath = ""
            }
        }

        // Split path from query
        val qIdx = rawPath.indexOf('?')
        val path: String
        val queryString: String
        if (qIdx >= 0) {
            path = rawPath.substring(0, qIdx)
            queryString = rawPath.substring(qIdx + 1)
        } else {
            path = rawPath
            queryString = ""
        }

        val pathSegments = path
            .split('/')
            .filter { it.isNotEmpty() }
            .map { decodeComponent(it) }

        val queryParams = parseQuery(queryString)

        return DeepLink(
            raw = uri,
            scheme = scheme,
            host = host,
            path = path,
            pathSegments = pathSegments,
            queryParams = queryParams,
            fragment = fragment,
        )
    }

    /** Percent-encode a single URI component value. */
    fun encodeComponent(value: String): String = buildString {
        for (ch in value) {
            if (ch.isUnreserved()) {
                append(ch)
            } else {
                val bytes = ch.toString().encodeToByteArray()
                for (b in bytes) {
                    append('%')
                    append(HEX[(b.toInt() shr 4) and 0xF])
                    append(HEX[b.toInt() and 0xF])
                }
            }
        }
    }

    /** Percent-decode a single URI component value. Returns [encoded] unchanged on malformed input. */
    fun decodeComponent(encoded: String): String {
        if (!encoded.contains('%')) return encoded
        return buildString {
            var i = 0
            while (i < encoded.length) {
                val ch = encoded[i]
                if (ch == '%' && i + 2 < encoded.length) {
                    val hi = encoded[i + 1].digitToIntOrNull(16)
                    val lo = encoded[i + 2].digitToIntOrNull(16)
                    if (hi != null && lo != null) {
                        append(((hi shl 4) or lo).toChar())
                        i += 3
                        continue
                    }
                }
                append(ch)
                i++
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private fun empty(raw: String) = DeepLink(
        raw = raw,
        scheme = "",
        host = "",
        path = "",
        pathSegments = emptyList(),
        queryParams = emptyMap(),
        fragment = null,
    )

    private fun splitFragment(uri: String): Pair<String, String?> {
        val idx = uri.indexOf('#')
        return if (idx >= 0) {
            uri.substring(0, idx) to uri.substring(idx + 1).ifEmpty { null }
        } else {
            uri to null
        }
    }

    private fun parseQuery(queryString: String): Map<String, String> {
        if (queryString.isBlank()) return emptyMap()
        return buildMap {
            for (pair in queryString.split('&')) {
                val eqIdx = pair.indexOf('=')
                if (eqIdx >= 0) {
                    val key = decodeComponent(pair.substring(0, eqIdx))
                    val value = decodeComponent(pair.substring(eqIdx + 1))
                    if (key.isNotEmpty()) put(key, value)
                } else {
                    val key = decodeComponent(pair)
                    if (key.isNotEmpty()) put(key, "")
                }
            }
        }
    }

    private fun Char.isUnreserved() = this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' ||
        this == '-' || this == '_' || this == '.' || this == '~'

    private val HEX = "0123456789ABCDEF"
}
