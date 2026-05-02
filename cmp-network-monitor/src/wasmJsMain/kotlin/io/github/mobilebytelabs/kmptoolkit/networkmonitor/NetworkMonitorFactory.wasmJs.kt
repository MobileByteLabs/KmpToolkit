package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * WasmJS actual: uses [WasmJsNetworkMonitor] backed by navigator.onLine + online/offline events.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = WasmJsNetworkMonitor(config)
