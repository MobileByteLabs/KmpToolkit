package io.github.mobilebytelabs.kmptoolkit.networkmonitor

// MinGW has no built-in HTTP client; report as unsupported
internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    CaptivePortalResult.DetectionFailed("Captive portal detection not supported on Windows Native")
