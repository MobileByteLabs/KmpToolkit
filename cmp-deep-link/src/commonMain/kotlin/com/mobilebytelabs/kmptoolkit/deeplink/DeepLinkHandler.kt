package com.mobilebytelabs.kmptoolkit.deeplink

import com.mobilebytelabs.kmptoolkit.deeplink.internal.UriParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton entry point for all deep link events across the application.
 *
 * Platform code (Android Activity, iOS AppDelegate, JVM main, JS window) calls
 * [handle] when the OS delivers a deep link URI. All consumers collect [incoming]
 * on whatever dispatcher they prefer.
 *
 * **Thread safety**: [handle] and [clear] are safe to call from any thread.
 * [StateFlow] updates are atomic; [SharedFlow] emissions use an unbounded buffer.
 *
 * ## Usage
 *
 * ```kotlin
 * // Anywhere in shared code:
 * val job = scope.launch {
 *     DeepLinkHandler.incoming.collect { link ->
 *         println("Received: ${link.raw}")
 *     }
 * }
 *
 * // Platform entry points call:
 * DeepLinkHandler.handle("myapp://product/42?ref=banner")
 * ```
 */
object DeepLinkHandler {

    // replay=0: new subscribers see only future links (use lastReceived for the latest).
    // extraBufferCapacity=UNLIMITED: tryEmit() never drops values under concurrent load.
    private val _incoming = MutableSharedFlow<DeepLink>(replay = 0, extraBufferCapacity = Channel.UNLIMITED)
    private val _lastReceived = MutableStateFlow<DeepLink?>(null)

    /**
     * Hot [SharedFlow] that emits every parsed [DeepLink].
     * New subscribers receive only future links; use [lastReceived] for the latest value.
     */
    val incoming: SharedFlow<DeepLink> = _incoming.asSharedFlow()

    /**
     * [StateFlow] holding the most recently received [DeepLink], or `null` before
     * the first link arrives or after [clear] is called.
     */
    val lastReceived: StateFlow<DeepLink?> = _lastReceived.asStateFlow()

    /**
     * Parse [uri] and emit the resulting [DeepLink] on [incoming].
     *
     * Safe to call from any thread — state updates are atomic.
     *
     * @param uri The raw URI string delivered by the OS (e.g. `myapp://open?ref=x`).
     */
    fun handle(uri: String) {
        val link = UriParser.parse(uri)
        _lastReceived.value = link
        _incoming.tryEmit(link)
    }

    /**
     * Reset [lastReceived] to `null`. Does not affect in-flight [incoming] emissions.
     */
    fun clear() {
        _lastReceived.value = null
    }
}
