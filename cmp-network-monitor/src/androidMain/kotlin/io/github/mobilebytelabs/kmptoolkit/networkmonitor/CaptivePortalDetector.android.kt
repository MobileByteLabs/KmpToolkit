package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    withContext(Dispatchers.IO) {
        try {
            val conn = URI(config.validationUrl).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = config.validationTimeoutMs.toInt()
            conn.readTimeout = config.validationTimeoutMs.toInt()
            conn.instanceFollowRedirects = false
            conn.useCaches = false

            try {
                val code = conn.responseCode
                when {
                    code == 204 || code == 200 -> CaptivePortalResult.NoCaptivePortal

                    code in 300..399 -> {
                        val location = conn.getHeaderField("Location")
                        CaptivePortalResult.CaptivePortalDetected(redirectUrl = location)
                    }

                    else -> CaptivePortalResult.DetectionFailed("Unexpected response code: $code")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            CaptivePortalResult.DetectionFailed(e.message ?: "Unknown error")
        }
    }
