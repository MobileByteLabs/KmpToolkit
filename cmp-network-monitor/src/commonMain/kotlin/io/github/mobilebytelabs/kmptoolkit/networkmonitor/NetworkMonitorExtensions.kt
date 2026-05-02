package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Throws [NetworkUnavailableException] if currently offline.
 * Use at function entry to fail-fast on network-dependent operations.
 */
fun NetworkMonitor.requireOnline() {
    if (!isOnline.value) {
        throw NetworkUnavailableException()
    }
}

/**
 * Suspends until the device is online, then returns.
 * Returns immediately if already online.
 */
suspend fun NetworkMonitor.ensureOnline() {
    if (!isOnline.value) {
        isOnline.first { it }
    }
}

/**
 * Suspends until online, executes [block], and returns the result.
 * Throws [NetworkUnavailableException] if cancelled while waiting.
 */
suspend fun <T> NetworkMonitor.withNetworkGuard(block: suspend () -> T): T {
    ensureOnline()
    return block()
}

/**
 * Suspends until the device is online and returns the [NetworkInfo].
 * Returns immediately if already online.
 */
suspend fun NetworkMonitor.awaitOnline(): NetworkInfo {
    val status = networkStatus.first { it is NetworkStatus.Available }
    return (status as NetworkStatus.Available).info
}

/**
 * Returns a [kotlinx.coroutines.flow.Flow] that mirrors [networkStatus]
 * but completes when the device goes offline.
 */
fun NetworkMonitor.onlyWhileOnline() = networkStatus.transformWhile { status ->
    if (status is NetworkStatus.Available) {
        emit(status)
        true
    } else {
        false
    }
}

/**
 * Retries [action] each time the device reconnects after a failure.
 * Returns the first successful result.
 *
 * @param maxRetries Maximum number of retries (default 3). Use [Int.MAX_VALUE] for indefinite.
 * @param timeoutMs Overall timeout in milliseconds (default [Long.MAX_VALUE] = no timeout).
 * @param action The suspending action to retry.
 * @throws NetworkUnavailableException if [maxRetries] is exhausted or [timeoutMs] elapses.
 */
suspend fun <T> NetworkMonitor.retryOnReconnect(
    maxRetries: Int = 3,
    timeoutMs: Long = Long.MAX_VALUE,
    action: suspend () -> T,
): T {
    val result = withTimeoutOrNull(timeoutMs) {
        var lastException: Throwable? = null
        repeat(maxRetries) {
            try {
                ensureOnline()
                return@withTimeoutOrNull action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                // Wait for next reconnect event
                isOnline.first { !it } // wait to go offline
                isOnline.first { it } // wait to come back online
            }
        }
        throw NetworkUnavailableException(
            message = "Failed after $maxRetries retries",
            cause = lastException,
        )
    }
    return result ?: throw NetworkUnavailableException(
        message = "Retry timed out after ${timeoutMs}ms",
    )
}

/**
 * Returns `true` if currently connected via the specified [NetworkType].
 */
fun NetworkMonitor.isConnectedVia(type: NetworkType): Boolean {
    val status = networkStatus.value
    return status is NetworkStatus.Available && status.info.type == type
}
