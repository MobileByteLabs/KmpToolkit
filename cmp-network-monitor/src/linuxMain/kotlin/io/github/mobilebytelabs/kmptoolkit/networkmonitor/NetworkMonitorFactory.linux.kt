package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Linux actual: uses [LinuxNetworkMonitor] backed by getifaddrs() polling.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = LinuxNetworkMonitor(config)
