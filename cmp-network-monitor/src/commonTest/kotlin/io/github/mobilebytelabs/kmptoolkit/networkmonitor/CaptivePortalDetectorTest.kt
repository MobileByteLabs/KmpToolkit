package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class CaptivePortalDetectorTest {

    @Test
    fun noCaptivePortalResult() {
        val result = CaptivePortalResult.NoCaptivePortal
        assertIs<CaptivePortalResult.NoCaptivePortal>(result)
    }

    @Test
    fun captivePortalDetectedWithUrl() {
        val result = CaptivePortalResult.CaptivePortalDetected(
            redirectUrl = "https://login.wifi.example.com",
        )
        assertIs<CaptivePortalResult.CaptivePortalDetected>(result)
        assertEquals("https://login.wifi.example.com", result.redirectUrl)
    }

    @Test
    fun captivePortalDetectedWithoutUrl() {
        val result = CaptivePortalResult.CaptivePortalDetected()
        assertIs<CaptivePortalResult.CaptivePortalDetected>(result)
        assertNull(result.redirectUrl)
    }

    @Test
    fun detectionFailedWithReason() {
        val result = CaptivePortalResult.DetectionFailed("Timeout")
        assertIs<CaptivePortalResult.DetectionFailed>(result)
        assertEquals("Timeout", result.reason)
    }

    @Test
    fun captivePortalStatusIsNotOnline() {
        val status = NetworkStatus.CaptivePortal(
            info = NetworkInfo(type = NetworkType.WiFi),
            redirectUrl = "https://login.example.com",
        )
        assertEquals(false, status.isOnline)
    }

    @Test
    fun captivePortalQualityIsPoor() {
        val status = NetworkStatus.CaptivePortal(
            info = NetworkInfo(type = NetworkType.WiFi),
        )
        assertEquals(NetworkQuality.Poor, status.toQuality())
    }
}
