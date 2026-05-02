package io.github.mobilebytelabs.kmptoolkit.networkmonitor

// WasmJS has limited fetch API access; provide a stub that reports detection as unsupported
internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    CaptivePortalResult.DetectionFailed("Captive portal detection not supported on WasmJS")
