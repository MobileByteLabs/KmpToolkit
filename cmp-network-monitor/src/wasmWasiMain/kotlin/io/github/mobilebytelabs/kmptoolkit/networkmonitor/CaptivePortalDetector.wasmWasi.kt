package io.github.mobilebytelabs.kmptoolkit.networkmonitor

internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    CaptivePortalResult.DetectionFailed("Captive portal detection not supported on WasmWASI")
