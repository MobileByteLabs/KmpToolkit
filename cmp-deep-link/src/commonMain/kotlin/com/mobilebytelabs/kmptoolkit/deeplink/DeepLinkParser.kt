package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.RoutePattern
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer

/**
 * Declarative, type-safe route matching DSL for deep link URIs.
 *
 * Build a parser once (e.g. at app startup), then call [parse] on each incoming [DeepLink].
 *
 * ## Example
 *
 * ```kotlin
 * @Serializable
 * data class ProductRoute(val id: String)
 *
 * @Serializable
 * data class ProfileRoute(val username: String, val tab: String = "posts")
 *
 * val parser = deepLinkParser {
 *     route<ProductRoute>("/product/{id}")
 *     route<ProfileRoute>("/user/{username}/posts")
 * }
 *
 * // Later, when a link arrives:
 * val product: ProductRoute? = parser.parse(link)
 * ```
 *
 * **Constraints:**
 * - Zero reflection — serialization uses `reified inline` + [serializer] only.
 * - All param values are strings; the JSON codec converts them to the target field type.
 * - Query params supplement path params: `?tab=videos` populates `tab` in the target class.
 */
fun deepLinkParser(block: DeepLinkParserBuilder.() -> Unit): DeepLinkParser =
    DeepLinkParserBuilder().apply(block).build()

// ============================================================================
// Builder
// ============================================================================

/** DSL builder for [DeepLinkParser]. Use via [deepLinkParser]. */
class DeepLinkParserBuilder internal constructor() {

    @PublishedApi
    internal val routes = mutableListOf<RouteEntry<*>>()

    /**
     * Register a route that maps a URI path pattern to a [T] data class.
     *
     * Path params (e.g. `{id}`) and query params are merged, then decoded via
     * [kotlinx.serialization] into [T]. Field names must match param names.
     *
     * @param T Serializable data class representing this route's parameters.
     * @param pattern URI path pattern (e.g. `/product/{id}`, `/user/{username?}`).
     */
    inline fun <reified T : Any> route(pattern: String) {
        addRoute(pattern, serializer<T>())
    }

    @PublishedApi
    internal fun <T : Any> addRoute(pattern: String, kSerializer: KSerializer<T>) {
        routes += RouteEntry(pattern = RoutePattern(pattern), serializer = kSerializer)
    }

    internal fun build() = DeepLinkParser(routes.toList())
}

// ============================================================================
// Parser
// ============================================================================

/**
 * Immutable parser produced by [deepLinkParser]. Thread-safe (no mutable state).
 */
class DeepLinkParser internal constructor(private val routes: List<RouteEntry<*>>) {

    /**
     * Attempt to match [deepLink] against all registered routes in declaration order.
     *
     * Returns the first match decoded as [T], or `null` if no route matches or
     * the matched route targets a different type.
     *
     * @param T The expected route data class.
     */
    inline fun <reified T : Any> parse(deepLink: DeepLink): T? = parseInternal(deepLink, serializer<T>())

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Any> parseInternal(deepLink: DeepLink, target: KSerializer<T>): T? {
        for (entry in routes) {
            val params = entry.pattern.match(deepLink.pathSegments) ?: continue
            if (entry.serializer.descriptor != target.descriptor) continue

            // Merge path params + query params (path params win on conflict)
            val merged = deepLink.queryParams + params

            // Build a JsonObject from the merged string map and decode
            val jsonObject = JsonObject(merged.mapValues { (_, v) -> JsonPrimitive(v) })
            return try {
                @Suppress("UNCHECKED_CAST")
                json.decodeFromJsonElement(target, jsonObject)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }
}

// ============================================================================
// Internal model
// ============================================================================

internal class RouteEntry<T : Any>(val pattern: RoutePattern, val serializer: KSerializer<T>)
