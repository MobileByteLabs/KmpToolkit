package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.await
import org.w3c.fetch.RequestInit

internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult = try {
    val init = RequestInit(
        method = "HEAD",
        redirect = "manual".asDynamic().unsafeCast<org.w3c.fetch.RequestRedirect>(),
    )

    val response = kotlinx.browser.window.fetch(config.validationUrl, init).await()
    val code = response.status.toInt()
    when {
        code == 204 || code == 200 -> CaptivePortalResult.NoCaptivePortal

        code == 0 -> {
            // Opaque redirect response (redirect = "manual")
            CaptivePortalResult.CaptivePortalDetected()
        }

        code in 300..399 -> {
            val location = response.headers.get("Location")
            CaptivePortalResult.CaptivePortalDetected(redirectUrl = location)
        }

        else -> CaptivePortalResult.DetectionFailed("Unexpected response code: $code")
    }
} catch (e: Throwable) {
    CaptivePortalResult.DetectionFailed(e.message ?: "Unknown error")
}
