package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Result of a captive portal detection check.
 */
sealed class CaptivePortalResult {
    /** No captive portal detected — internet is freely available. */
    data object NoCaptivePortal : CaptivePortalResult()

    /** Captive portal detected — HTTP validation returned a redirect. */
    data class CaptivePortalDetected(val redirectUrl: String? = null) : CaptivePortalResult()

    /** Detection failed (timeout, DNS failure, etc.). */
    data class DetectionFailed(val reason: String) : CaptivePortalResult()
}

/**
 * Extension to check the current captive portal status.
 *
 * Returns [CaptivePortalResult.NoCaptivePortal] if the validation URL returns 204/200
 * without redirects, [CaptivePortalResult.CaptivePortalDetected] if a redirect (302/307)
 * is detected, or [CaptivePortalResult.DetectionFailed] on error.
 *
 * This is a suspend function that performs a network request. Prefer using it
 * sparingly — it's designed for one-shot checks, not continuous monitoring.
 */
suspend fun NetworkMonitor.detectCaptivePortal(): CaptivePortalResult {
    if (!isOnline.value) {
        return CaptivePortalResult.DetectionFailed("Device is offline")
    }
    return platformDetectCaptivePortal(
        (networkStatus.value as? NetworkStatus.Available)?.info?.let {
            NetworkMonitorConfig()
        } ?: NetworkMonitorConfig(),
    )
}

/**
 * Platform-specific captive portal detection.
 * Each platform implements HTTP HEAD with redirect detection.
 */
internal expect suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult
