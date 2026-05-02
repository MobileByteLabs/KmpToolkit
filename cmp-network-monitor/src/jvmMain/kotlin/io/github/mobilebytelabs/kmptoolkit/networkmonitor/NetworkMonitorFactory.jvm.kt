package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * JVM actual: uses [JvmNetworkMonitor] backed by polling + HTTP validation.
 * Poll interval and validation strategy are configurable via [config].
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = JvmNetworkMonitor(config)
