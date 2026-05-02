package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Configuration for [NetworkMonitor] behavior.
 *
 * @property pollIntervalMs Polling interval for platforms without push-based APIs (JVM, Linux, Windows).
 *   Ignored on Android (callback-based) and Apple (NWPathMonitor push).
 * @property validationUrl URL for HTTP HEAD validation. Used when [validationStrategy] includes HTTP.
 *   Default uses Google's generate_204 endpoint (returns 204 with a tiny response).
 *   Override for China: use a local CDN. Override for corporate: use an intranet URL.
 * @property validationTimeoutMs Timeout for HTTP validation requests in milliseconds.
 * @property validationStrategy How to determine "online" status. See [ValidationStrategy].
 */
data class NetworkMonitorConfig(
    val pollIntervalMs: Long = 3_000L,
    val validationUrl: String = "https://clients3.google.com/generate_204",
    val validationTimeoutMs: Long = 5_000L,
    val validationStrategy: ValidationStrategy = ValidationStrategy.NativeThenHttp,
)

/**
 * Strategy for determining whether the device has actual internet connectivity.
 */
enum class ValidationStrategy {
    /** Platform native API only. Fast but may false-positive on captive portals. */
    NativeOnly,

    /** HTTP HEAD request only. Reliable but adds latency and network traffic. */
    HttpOnly,

    /** Native first, HTTP confirms. Best accuracy, slightly slower. DEFAULT. */
    NativeThenHttp,
}
