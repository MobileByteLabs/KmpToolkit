package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.min
import kotlin.math.pow

/**
 * Configurable retry policy for network-dependent operations.
 *
 * Usage:
 * ```kotlin
 * val policy = RetryPolicy {
 *     maxAttempts = 5
 *     backoff = BackoffStrategy.Exponential(baseMs = 1000, maxMs = 30_000)
 *     onlyWhen(NetworkType.WiFi, NetworkType.Ethernet)
 *     timeoutMs = 60_000
 * }
 *
 * val result = monitor.executeWithRetry(policy) {
 *     api.fetchData()
 * }
 * ```
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val backoff: BackoffStrategy = BackoffStrategy.Exponential(),
    val allowedTypes: Set<NetworkType>? = null,
    val timeoutMs: Long = Long.MAX_VALUE,
) {
    companion object {
        operator fun invoke(builder: RetryPolicyBuilder.() -> Unit): RetryPolicy =
            RetryPolicyBuilder().apply(builder).build()
    }
}

/**
 * Strategy for calculating delay between retry attempts.
 */
sealed class BackoffStrategy {

    /** Constant delay between retries. */
    data class Fixed(val delayMs: Long = 1_000L) : BackoffStrategy()

    /**
     * Exponential backoff with optional jitter.
     *
     * @param baseMs Base delay for first retry.
     * @param maxMs Maximum delay cap.
     * @param multiplier Growth factor per attempt.
     */
    data class Exponential(val baseMs: Long = 1_000L, val maxMs: Long = 30_000L, val multiplier: Double = 2.0) :
        BackoffStrategy()

    /** No delay between retries. */
    data object None : BackoffStrategy()

    internal fun delayForAttempt(attempt: Int): Long = when (this) {
        is Fixed -> delayMs

        is Exponential -> min(
            (baseMs * multiplier.pow(attempt.toDouble())).toLong(),
            maxMs,
        )

        is None -> 0L
    }
}

/**
 * DSL builder for [RetryPolicy].
 */
class RetryPolicyBuilder {
    var maxAttempts: Int = 3
    var backoff: BackoffStrategy = BackoffStrategy.Exponential()
    var timeoutMs: Long = Long.MAX_VALUE
    private var allowedTypes: MutableSet<NetworkType>? = null

    /** Only retry when connected via one of these network types. */
    fun onlyWhen(vararg types: NetworkType) {
        allowedTypes = (allowedTypes ?: mutableSetOf()).apply { addAll(types) }
    }

    internal fun build() = RetryPolicy(
        maxAttempts = maxAttempts,
        backoff = backoff,
        allowedTypes = allowedTypes,
        timeoutMs = timeoutMs,
    )
}

/**
 * Execute [action] with retry according to [policy].
 * Waits for reconnection between attempts if offline.
 *
 * @throws NetworkUnavailableException if all attempts exhausted or timeout reached.
 */
suspend fun <T> NetworkMonitor.executeWithRetry(policy: RetryPolicy, action: suspend () -> T): T {
    val result = withTimeoutOrNull(policy.timeoutMs) {
        var lastException: Throwable? = null
        repeat(policy.maxAttempts) { attempt ->
            try {
                // Wait for suitable connection
                ensureOnline()

                // Check type constraint
                if (policy.allowedTypes != null) {
                    val status = networkStatus.value
                    if (status is NetworkStatus.Available && status.info.type !in policy.allowedTypes) {
                        // Wait for a suitable connection type
                        networkStatus.first { s ->
                            s is NetworkStatus.Available && s.info.type in policy.allowedTypes
                        }
                    }
                }

                return@withTimeoutOrNull action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                val delayMs = policy.backoff.delayForAttempt(attempt)
                if (delayMs > 0) delay(delayMs)

                // If offline, wait for reconnect
                if (!isOnline.value) {
                    isOnline.first { it }
                }
            }
        }
        throw NetworkUnavailableException(
            message = "Failed after ${policy.maxAttempts} attempts",
            cause = lastException,
        )
    }
    return result ?: throw NetworkUnavailableException(
        message = "Retry timed out after ${policy.timeoutMs}ms",
    )
}
