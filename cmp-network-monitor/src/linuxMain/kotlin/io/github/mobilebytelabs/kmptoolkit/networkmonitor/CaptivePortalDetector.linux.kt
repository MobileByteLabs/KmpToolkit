package io.github.mobilebytelabs.kmptoolkit.networkmonitor

// Linux Native has no built-in HTTP client; report as unsupported
// In production, users should use NativeThenHttp strategy which handles this at monitor level
internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    CaptivePortalResult.DetectionFailed("Captive portal detection not supported on Linux Native")
