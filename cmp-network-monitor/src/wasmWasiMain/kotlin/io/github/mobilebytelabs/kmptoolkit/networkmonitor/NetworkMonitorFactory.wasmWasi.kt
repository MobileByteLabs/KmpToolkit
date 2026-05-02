package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * WasmWASI actual: no-op stub.
 *
 * WASI has no network observation API. Returns a stub that always
 * reports as online. Future WASI networking proposals may enable
 * real implementation.
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor = StubNetworkMonitor()
