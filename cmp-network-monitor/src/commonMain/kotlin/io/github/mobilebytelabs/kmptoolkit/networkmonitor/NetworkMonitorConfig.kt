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
 * @property backgroundPollIntervalMs Polling interval when app is in background. Only used by
 *   polling-based platforms (JVM, Linux, Windows). Default 30s.
 * @property maxValidationBackoffMs Maximum backoff interval when HTTP validation fails repeatedly.
 *   Prevents hammering the validation endpoint. Default 60s.
 * @property captivePortalDetection Whether to detect captive portals (hotel WiFi, etc.).
 *   When enabled, HTTP validation checks for 302/307 redirects and reports
 *   [NetworkStatus.CaptivePortal] instead of offline. Default false (opt-in).
 */
data class NetworkMonitorConfig(
    val pollIntervalMs: Long = 3_000L,
    val validationUrl: String = "https://clients3.google.com/generate_204",
    val validationTimeoutMs: Long = 5_000L,
    val validationStrategy: ValidationStrategy = ValidationStrategy.NativeThenHttp,
    val backgroundPollIntervalMs: Long = 30_000L,
    val maxValidationBackoffMs: Long = 60_000L,
    val captivePortalDetection: Boolean = false,
) {
    companion object {
        /**
         * Build a config using DSL syntax.
         *
         * ```kotlin
         * val config = NetworkMonitorConfig {
         *     pollIntervalMs = 5_000L
         *     validationStrategy = ValidationStrategy.NativeOnly
         * }
         * ```
         */
        operator fun invoke(builder: Builder.() -> Unit): NetworkMonitorConfig = Builder().apply(builder).build()
    }

    class Builder {
        var pollIntervalMs: Long = 3_000L
        var validationUrl: String = "https://clients3.google.com/generate_204"
        var validationTimeoutMs: Long = 5_000L
        var validationStrategy: ValidationStrategy = ValidationStrategy.NativeThenHttp
        var backgroundPollIntervalMs: Long = 30_000L
        var maxValidationBackoffMs: Long = 60_000L
        var captivePortalDetection: Boolean = false

        internal fun build() = NetworkMonitorConfig(
            pollIntervalMs = pollIntervalMs,
            validationUrl = validationUrl,
            validationTimeoutMs = validationTimeoutMs,
            validationStrategy = validationStrategy,
            backgroundPollIntervalMs = backgroundPollIntervalMs,
            maxValidationBackoffMs = maxValidationBackoffMs,
            captivePortalDetection = captivePortalDetection,
        )
    }
}

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
