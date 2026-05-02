package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Apple actual: uses [AppleNetworkMonitor] backed by NWPathMonitor.
 * Works on iOS, macOS, tvOS, and watchOS.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = AppleNetworkMonitor(config)
