/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.deeplink

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Injectable deep-link intake — the type to depend on from a ViewModel or navigation coordinator.
 *
 * ## Why an interface when [DeepLinkHandler] already exists
 * [DeepLinkHandler] is a process-wide `object`, which is the right shape for it: OS callbacks
 * arrive with no reference to your object graph, so intake has to be reachable from anywhere.
 * That same property makes it awkward to test — an `object` cannot be substituted, and state
 * leaks between test cases because there is only ever one of it.
 *
 * [DeepLinkManager] is the same capability behind an injectable type. Production code depends on
 * the interface; [DeepLinkManagerImpl] forwards to the singleton, and `FakeDeepLinkManager` gives
 * each test its own isolated instance.
 *
 * ## Using
 * ```kotlin
 * class NavCoordinator(private val links: DeepLinkManager, scope: CoroutineScope) {
 *     private val parser = deepLinkParser {
 *         route("/product/{id}") { ProductScreen(it["id"]!!) }
 *     }
 *
 *     init {
 *         scope.launch {
 *             links.incoming.collect { link -> parser.parse(link)?.let(::navigate) }
 *         }
 *     }
 * }
 * ```
 */
public interface DeepLinkManager {

    /** Links as they arrive. New subscribers see only future links — see [lastReceived]. */
    public val incoming: SharedFlow<DeepLink>

    /**
     * The most recent link, or `null` if none has arrived.
     *
     * Read this on startup: a link that launched the app is delivered before your collector
     * exists, so relying on [incoming] alone drops the cold-start case — the one that matters most.
     */
    public val lastReceived: StateFlow<DeepLink?>

    /**
     * Feed a URI in.
     *
     * Called by the platform intake on Android, iOS, macOS and the browser; call it yourself on
     * targets with no OS delivery (watchOS), or to replay a link in a test.
     */
    public fun handle(uri: String)

    /** Forget [lastReceived], so a consumed link is not re-processed on the next subscription. */
    public fun clear()
}

/**
 * The one [DeepLinkManager] — forwards to the process-wide [DeepLinkHandler].
 *
 * Stateless in itself; all state lives in the singleton, which is what lets an OS callback reach
 * the same stream your injected instance exposes.
 */
public class DeepLinkManagerImpl : DeepLinkManager {

    override val incoming: SharedFlow<DeepLink> get() = DeepLinkHandler.incoming

    override val lastReceived: StateFlow<DeepLink?> get() = DeepLinkHandler.lastReceived

    override fun handle(uri: String) {
        DeepLinkHandler.handle(uri)
        // Scheme only. A deep-link path and query ARE the payload — order ids, reset tokens,
        // invite codes — so nothing beyond the scheme is reported.
        observeLifecycle(
            cmpMetadata(),
            "deep_link_handled",
            mapOf("scheme" to uri.substringBefore("://", missingDelimiterValue = "none")),
        )
    }

    override fun clear() {
        DeepLinkHandler.clear()
    }
}
