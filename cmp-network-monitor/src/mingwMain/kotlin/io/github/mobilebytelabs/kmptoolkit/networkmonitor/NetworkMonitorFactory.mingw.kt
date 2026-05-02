package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Windows (MinGW) actual: uses [MingwNetworkMonitor] backed by polling.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = MingwNetworkMonitor(config)
