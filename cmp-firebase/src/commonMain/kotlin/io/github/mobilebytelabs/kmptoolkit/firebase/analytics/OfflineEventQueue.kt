/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * An [AnalyticsHelper] decorator that guarantees no event loss while offline: events logged
 * while [monitor] reports offline are buffered (bounded) and flushed to [delegate] on
 * reconnect. Wrap your real helper with it:
 *
 * ```kotlin
 * val analytics = OfflineEventQueue(FirebaseAnalyticsHelper(...), networkMonitor, appScope)
 * ```
 *
 * User-property / user-id / collection / consent calls pass straight through to [delegate].
 */
class OfflineEventQueue(
    private val delegate: AnalyticsHelper,
    private val monitor: NetworkMonitor,
    scope: CoroutineScope,
    private val maxBuffered: Int = DEFAULT_MAX_BUFFERED,
) : AnalyticsHelper {

    private val buffer = ArrayDeque<AnalyticsEvent>()

    init {
        // Drain on the offline → online edge.
        var wasOnline = monitor.isOnline.value
        monitor.isOnline
            .onEach { online ->
                if (online && !wasOnline) flush()
                wasOnline = online
            }
            .launchIn(scope)
    }

    override fun logEvent(event: AnalyticsEvent) {
        if (monitor.isOnline.value) {
            delegate.logEvent(event)
        } else {
            if (buffer.size >= maxBuffered) buffer.removeFirst() // bounded — drop oldest
            buffer.addLast(event)
        }
    }

    /** Emit everything buffered, oldest-first, then clear. */
    fun flush() {
        while (buffer.isNotEmpty()) delegate.logEvent(buffer.removeFirst())
    }

    /** Events currently buffered (awaiting reconnect). */
    val bufferedCount: Int get() = buffer.size

    override fun setUserProperty(name: String, value: String) = delegate.setUserProperty(name, value)
    override fun setUserId(userId: String) = delegate.setUserId(userId)
    override fun setCollectionEnabled(enabled: Boolean) = delegate.setCollectionEnabled(enabled)
    override fun setConsent(analyticsStorage: Boolean, adStorage: Boolean) =
        delegate.setConsent(analyticsStorage, adStorage)

    private companion object {
        const val DEFAULT_MAX_BUFFERED = 500
    }
}
