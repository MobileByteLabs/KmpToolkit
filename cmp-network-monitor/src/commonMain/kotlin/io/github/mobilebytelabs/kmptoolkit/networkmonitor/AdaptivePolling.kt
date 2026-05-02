package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.math.min

/**
 * Adaptive polling state that adjusts poll intervals based on network stability.
 *
 * When the network is stable (no changes), the interval gradually increases
 * toward [maxIntervalMs]. When a change is detected, it resets to [baseIntervalMs].
 *
 * Used internally by polling-based monitors (JVM, Linux, MinGW).
 */
internal class AdaptivePollingState(
    private val baseIntervalMs: Long,
    private val maxIntervalMs: Long,
    private val growthFactor: Double = 1.5,
) {
    private var currentIntervalMs: Long = baseIntervalMs
    private var consecutiveStableChecks: Int = 0

    /**
     * Report a stable check (no network change detected).
     * Grows the interval toward [maxIntervalMs].
     */
    fun reportStable() {
        consecutiveStableChecks++
        if (consecutiveStableChecks >= 3) {
            currentIntervalMs = min(
                (currentIntervalMs * growthFactor).toLong(),
                maxIntervalMs,
            )
        }
    }

    /**
     * Report a change (network state changed).
     * Resets interval to [baseIntervalMs] for responsive detection.
     */
    fun reportChange() {
        consecutiveStableChecks = 0
        currentIntervalMs = baseIntervalMs
    }

    /**
     * Current polling interval in milliseconds.
     */
    val intervalMs: Long get() = currentIntervalMs

    /**
     * Reset to initial state.
     */
    fun reset() {
        currentIntervalMs = baseIntervalMs
        consecutiveStableChecks = 0
    }
}

/**
 * Validation backoff state that increases delay when HTTP validation fails repeatedly.
 *
 * Prevents hammering the validation endpoint when it's unreachable.
 * Resets on success.
 */
internal class ValidationBackoffState(
    private val baseDelayMs: Long = 1_000L,
    private val maxDelayMs: Long = 60_000L,
    private val multiplier: Double = 2.0,
) {
    private var consecutiveFailures: Int = 0
    private var currentDelayMs: Long = 0L

    /**
     * Report a validation failure. Increases the backoff delay.
     */
    fun reportFailure() {
        consecutiveFailures++
        currentDelayMs = min(
            (baseDelayMs * pow(multiplier, consecutiveFailures - 1)).toLong(),
            maxDelayMs,
        )
    }

    /**
     * Report a validation success. Resets the backoff.
     */
    fun reportSuccess() {
        consecutiveFailures = 0
        currentDelayMs = 0L
    }

    /**
     * Current backoff delay in milliseconds. 0 means no backoff.
     */
    val delayMs: Long get() = currentDelayMs

    /**
     * Whether we should skip this validation cycle (backoff active).
     */
    val shouldBackoff: Boolean get() = currentDelayMs > 0

    private fun pow(base: Double, exp: Int): Double {
        var result = 1.0
        repeat(exp) { result *= base }
        return result
    }
}
