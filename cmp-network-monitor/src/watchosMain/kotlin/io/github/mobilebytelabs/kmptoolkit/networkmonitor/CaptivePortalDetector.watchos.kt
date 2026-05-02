package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.convert
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.resume

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual suspend fun platformDetectCaptivePortal(config: NetworkMonitorConfig): CaptivePortalResult =
    suspendCancellableCoroutine { cont ->
        val url = NSURL.URLWithString(config.validationUrl) ?: run {
            cont.resume(CaptivePortalResult.DetectionFailed("Invalid URL"))
            return@suspendCancellableCoroutine
        }

        val request = NSMutableURLRequest.requestWithURL(url).apply {
            setHTTPMethod("GET")
            setTimeoutInterval(config.validationTimeoutMs / 1000.0)
            setValue("no-cache", forHTTPHeaderField = "Cache-Control")
        }

        val sessionConfig = NSURLSessionConfiguration.ephemeralSessionConfiguration
        val session = NSURLSession.sessionWithConfiguration(sessionConfig)

        val task = session.dataTaskWithRequest(request) { _: NSData?, response: NSURLResponse?, error: NSError? ->
            if (error != null) {
                cont.resume(CaptivePortalResult.DetectionFailed(error.localizedDescription))
                return@dataTaskWithRequest
            }

            val httpResponse = response as? NSHTTPURLResponse
            if (httpResponse == null) {
                cont.resume(CaptivePortalResult.DetectionFailed("No HTTP response"))
                return@dataTaskWithRequest
            }

            val code: Int = httpResponse.statusCode.convert()
            when {
                code == 204 || code == 200 -> cont.resume(CaptivePortalResult.NoCaptivePortal)

                code in 300..399 -> {
                    val location = httpResponse.allHeaderFields["Location"] as? String
                    cont.resume(CaptivePortalResult.CaptivePortalDetected(redirectUrl = location))
                }

                else -> cont.resume(CaptivePortalResult.DetectionFailed("Unexpected response code: $code"))
            }
        }

        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }
