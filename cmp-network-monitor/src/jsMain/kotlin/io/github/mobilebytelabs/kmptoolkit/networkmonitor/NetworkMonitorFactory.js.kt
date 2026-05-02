package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * JS actual: uses [JsNetworkMonitor] backed by navigator.onLine + online/offline events.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = JsNetworkMonitor(config)
