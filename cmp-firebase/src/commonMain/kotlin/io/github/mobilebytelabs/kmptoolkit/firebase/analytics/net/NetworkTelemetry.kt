/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics.net

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Auto-log network **transition** events (`network.transition.online_to_offline` /
 * `offline_to_online`) by observing a [NetworkMonitor]. Opt-in wiring — call once at startup;
 * the returned [Job] can be cancelled to stop.
 *
 * ```kotlin
 * analytics.attachNetworkTelemetry(networkMonitor, appScope)
 * ```
 */
fun AnalyticsHelper.attachNetworkTelemetry(
    monitor: NetworkMonitor,
    scope: CoroutineScope,
    enabled: () -> Boolean = { true },
): Job {
    var wasOnline: Boolean? = null
    return monitor.networkStatus
        .onEach { status ->
            if (!enabled()) return@onEach
            val online = status.isOnline
            val prev = wasOnline
            wasOnline = online
            if (prev != null && prev != online) {
                val event = if (online) EVENT_OFFLINE_TO_ONLINE else EVENT_ONLINE_TO_OFFLINE
                logEvent(AnalyticsEvent(event))
            }
        }
        .launchIn(scope)
}

const val EVENT_ONLINE_TO_OFFLINE: String = "network.transition.online_to_offline"
const val EVENT_OFFLINE_TO_ONLINE: String = "network.transition.offline_to_online"
