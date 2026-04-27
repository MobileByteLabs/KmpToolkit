package com.mobilebytelabs.kmptoolkit.deeplink.internal

/**
 * Compiled route pattern that matches URI paths and extracts named parameters.
 *
 * **Syntax:**
 * - `/product/{id}` — required named segment
 * - `/user/{username?}` — optional named segment (matches even if segment absent)
 * - `/search/{*query}` — catch-all: matches zero or more remaining segments, joined with `/`
 *
 * Zero reflection. Pure string operations.
 */
internal class RoutePattern(private val pattern: String) {

    private sealed interface Segment {
        data class Literal(val value: String) : Segment
        data class Required(val name: String) : Segment
        data class Optional(val name: String) : Segment
        data class CatchAll(val name: String) : Segment
    }

    private val segments: List<Segment> = pattern
        .split('/')
        .filter { it.isNotEmpty() }
        .map { seg ->
            when {
                seg.startsWith("{*") && seg.endsWith("}") ->
                    Segment.CatchAll(seg.substring(2, seg.length - 1))

                seg.startsWith("{") && seg.endsWith("?}") ->
                    Segment.Optional(seg.substring(1, seg.length - 2))

                seg.startsWith("{") && seg.endsWith("}") ->
                    Segment.Required(seg.substring(1, seg.length - 1))

                else -> Segment.Literal(seg)
            }
        }

    /**
     * Match [pathSegments] against this pattern.
     *
     * @return extracted params map if the path matches, or `null` if it does not.
     */
    fun match(pathSegments: List<String>): Map<String, String>? {
        val params = mutableMapOf<String, String>()
        var pathIdx = 0

        for (seg in segments) {
            when (seg) {
                is Segment.Literal -> {
                    if (pathIdx >= pathSegments.size) return null
                    if (pathSegments[pathIdx] != seg.value) return null
                    pathIdx++
                }

                is Segment.Required -> {
                    if (pathIdx >= pathSegments.size) return null
                    params[seg.name] = pathSegments[pathIdx]
                    pathIdx++
                }

                is Segment.Optional -> {
                    if (pathIdx < pathSegments.size) {
                        params[seg.name] = pathSegments[pathIdx]
                        pathIdx++
                    }
                    // optional: no match is also fine
                }

                is Segment.CatchAll -> {
                    // Consume all remaining segments
                    params[seg.name] = pathSegments.drop(pathIdx).joinToString("/")
                    pathIdx = pathSegments.size
                }
            }
        }

        // All path segments must be consumed (unless last pattern seg was CatchAll/Optional)
        val lastSeg = segments.lastOrNull()
        if (pathIdx < pathSegments.size &&
            lastSeg !is Segment.CatchAll
        ) {
            return null
        }

        return params
    }

    /**
     * Build a URI path from this pattern and supplied [params].
     * Catch-all and optional params: include if present in [params].
     */
    fun build(params: Map<String, String> = emptyMap()): String = buildString {
        for (seg in segments) {
            append('/')
            when (seg) {
                is Segment.Literal -> append(seg.value)

                is Segment.Required -> append(
                    UriParser.encodeComponent(params[seg.name] ?: error("Missing required param: ${seg.name}")),
                )

                is Segment.Optional -> {
                    val v = params[seg.name]
                    if (v != null) append(UriParser.encodeComponent(v))
                }

                is Segment.CatchAll -> {
                    val v = params[seg.name]
                    if (v != null) append(v) // already path-encoded by caller
                }
            }
        }
    }
}
