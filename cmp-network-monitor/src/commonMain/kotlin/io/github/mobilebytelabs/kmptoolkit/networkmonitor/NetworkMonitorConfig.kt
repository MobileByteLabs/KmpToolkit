package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Configuration for [NetworkMonitor] behavior.
 *
 * @property pollIntervalMs Polling interval for platforms without push-based APIs (JVM, Linux, Windows).
 *   Ignored on Android (callback-based) and Apple (NWPathMonitor push).
 * @property validationUrl Primary URL for HTTP validation. Used when [validationStrategy] includes HTTP.
 *   Default uses Google's generate_204 endpoint (returns HTTP 204 with an empty body — the sentinel
 *   the probe checks for; a captive portal returns 200 with a login page and is thus NOT validated).
 *   Override for China: use a local CDN. Override for corporate: use an intranet URL.
 * @property validationUrls Additional fallback endpoints probed with any-success semantics — the
 *   device is "online" if the primary OR any fallback returns the 204 sentinel. Guards against a
 *   single probe host being blocked/rate-limited/down (which would otherwise read as false-offline).
 *   The effective probe list is [validationUrl] followed by these, de-duplicated.
 * @property validationTimeoutMs Timeout for HTTP validation requests in milliseconds.
 * @property validationStrategy How to determine "online" status. See [ValidationStrategy].
 * @property backgroundPollIntervalMs Polling interval when app is in background. Only used by
 *   polling-based platforms (JVM, Linux, Windows). Default 30s.
 * @property maxValidationBackoffMs Maximum backoff interval when HTTP validation fails repeatedly.
 *   Prevents hammering the validation endpoint. Default 60s.
 * @property captivePortalDetection Whether to detect captive portals (hotel WiFi, etc.).
 *   When enabled, HTTP validation checks for 302/307 redirects and reports
 *   [NetworkStatus.CaptivePortal] instead of offline. Default false (opt-in).
 * @property revalidateWhileOnlineMs When > 0, re-runs the HTTP validation probe on this interval
 *   *while online* so a silently-dead connection (connected, but no real internet — e.g. a router
 *   that lost its uplink) is caught without user action. Default `0L` = disabled (push-based
 *   platforms stay zero-cost). Opt-in because it trades battery for freshness.
 * @property validationPort Port for the reachability probe (used by socket-based checks). Default 443.
 * @property validationMethod HTTP method for the validation probe. Default `HEAD` (cheap; the
 *   generate_204 sentinel supports it). Use `GET` for endpoints that only 204 on GET.
 * @property autoStart Whether the monitor begins observing on creation. Default `true` (existing
 *   behavior). Set `false` to construct now and call [NetworkMonitor.start] later.
 * @property onValidationResult Optional observability hook invoked for each validation probe with
 *   its [ValidationResult] (url, success, status, latency) — feed it to analytics/telemetry to
 *   answer "why does it think I'm offline?". Not part of value equality concerns for typical use.
 */
data class NetworkMonitorConfig(
    val pollIntervalMs: Long = 3_000L,
    val validationUrl: String = "https://clients3.google.com/generate_204",
    val validationUrls: List<String> = listOf(
        "https://connectivitycheck.gstatic.com/generate_204",
        "https://www.google.com/generate_204",
    ),
    val validationTimeoutMs: Long = 5_000L,
    val validationStrategy: ValidationStrategy = ValidationStrategy.NativeThenHttp,
    val backgroundPollIntervalMs: Long = 30_000L,
    val maxValidationBackoffMs: Long = 60_000L,
    val captivePortalDetection: Boolean = false,
    val revalidateWhileOnlineMs: Long = 0L,
    val validationPort: Int = 443,
    val validationMethod: String = "HEAD",
    // onValidationResult MUST NOT be the LAST constructor param: a trailing function-type parameter
    // would make `NetworkMonitorConfig { … }` bind to the constructor instead of the companion
    // `invoke(Builder.() -> Unit)` DSL. Keeping a non-function param (autoStart) last preserves the DSL.
    val onValidationResult: ((ValidationResult) -> Unit)? = null,
    val detectFiveG: Boolean = false,
    val autoStart: Boolean = true,
) {
    /** Effective probe list: primary [validationUrl] then [validationUrls], de-duplicated. */
    val effectiveValidationUrls: List<String>
        get() = (listOf(validationUrl) + validationUrls).distinct()

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
        var validationUrls: List<String> = listOf(
            "https://connectivitycheck.gstatic.com/generate_204",
            "https://www.google.com/generate_204",
        )
        var validationTimeoutMs: Long = 5_000L
        var validationStrategy: ValidationStrategy = ValidationStrategy.NativeThenHttp
        var backgroundPollIntervalMs: Long = 30_000L
        var maxValidationBackoffMs: Long = 60_000L
        var captivePortalDetection: Boolean = false
        var revalidateWhileOnlineMs: Long = 0L
        var validationPort: Int = 443
        var validationMethod: String = "HEAD"
        var autoStart: Boolean = true
        var onValidationResult: ((ValidationResult) -> Unit)? = null
        var detectFiveG: Boolean = false

        /** Ergonomic duration helpers: `pollIntervalMs = 5.seconds`, `revalidateWhileOnlineMs = 2.minutes`. */
        val Int.seconds: Long get() = this * 1_000L
        val Int.minutes: Long get() = this * 60_000L

        internal fun build() = NetworkMonitorConfig(
            pollIntervalMs = pollIntervalMs,
            validationUrl = validationUrl,
            validationUrls = validationUrls,
            validationTimeoutMs = validationTimeoutMs,
            validationStrategy = validationStrategy,
            backgroundPollIntervalMs = backgroundPollIntervalMs,
            maxValidationBackoffMs = maxValidationBackoffMs,
            captivePortalDetection = captivePortalDetection,
            revalidateWhileOnlineMs = revalidateWhileOnlineMs,
            validationPort = validationPort,
            validationMethod = validationMethod,
            autoStart = autoStart,
            onValidationResult = onValidationResult,
            detectFiveG = detectFiveG,
        )
    }
}

/**
 * Outcome of a single HTTP/socket validation probe — surfaced via
 * [NetworkMonitorConfig.onValidationResult] for diagnostics and telemetry.
 *
 * @property url the endpoint probed
 * @property success whether the probe met the "real internet" sentinel (e.g. HTTP 204)
 * @property statusCode HTTP status if known (null for socket-only checks)
 * @property latencyMs round-trip time in ms, or -1 if not measured
 */
data class ValidationResult(
    val url: String,
    val success: Boolean,
    val statusCode: Int? = null,
    val latencyMs: Long = -1L,
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
