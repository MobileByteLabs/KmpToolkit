package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * macOS implementation of HTTP client using NSURLSession.
 */
internal actual object HttpClient {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun get(url: String, headers: Map<String, String>): String {
        val nsUrl = NSURL.URLWithString(url)
            ?: throw IllegalArgumentException("Invalid URL: $url")

        val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
            setHTTPMethod("GET")
            headers.forEach { (key, value) ->
                setValue(value, forHTTPHeaderField = key)
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
                when {
                    error != null -> {
                        continuation.resumeWithException(
                            Exception("Network error: ${error.localizedDescription}"),
                        )
                    }

                    data != null -> {
                        val responseString = NSString.create(data = data, encoding = NSUTF8StringEncoding)
                            ?: throw Exception("Failed to decode response")
                        continuation.resume(responseString.toString())
                    }

                    else -> {
                        continuation.resumeWithException(Exception("No data received"))
                    }
                }
            }
            task.resume()

            continuation.invokeOnCancellation {
                task.cancel()
            }
        }
    }
}
