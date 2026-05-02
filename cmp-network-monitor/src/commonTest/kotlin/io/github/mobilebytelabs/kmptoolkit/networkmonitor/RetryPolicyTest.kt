package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetryPolicyTest {

    @Test
    fun defaultPolicyValues() {
        val policy = RetryPolicy()
        assertEquals(3, policy.maxAttempts)
        assertTrue(policy.backoff is BackoffStrategy.Exponential)
        assertNull(policy.allowedTypes)
        assertEquals(Long.MAX_VALUE, policy.timeoutMs)
    }

    @Test
    fun builderDsl() {
        val policy = RetryPolicy {
            maxAttempts = 5
            backoff = BackoffStrategy.Fixed(500L)
            onlyWhen(NetworkType.WiFi, NetworkType.Ethernet)
            timeoutMs = 30_000L
        }
        assertEquals(5, policy.maxAttempts)
        val backoff = policy.backoff
        assertTrue(backoff is BackoffStrategy.Fixed)
        assertEquals(500L, backoff.delayMs)
        val types = policy.allowedTypes
        assertNotNull(types)
        assertTrue(NetworkType.WiFi in types)
        assertTrue(NetworkType.Ethernet in types)
        assertEquals(30_000L, policy.timeoutMs)
    }

    @Test
    fun fixedBackoffDelay() {
        val strategy = BackoffStrategy.Fixed(1000L)
        assertEquals(1000L, strategy.delayForAttempt(0))
        assertEquals(1000L, strategy.delayForAttempt(5))
    }

    @Test
    fun exponentialBackoffGrows() {
        val strategy = BackoffStrategy.Exponential(baseMs = 1000L, maxMs = 10_000L, multiplier = 2.0)
        assertEquals(1000L, strategy.delayForAttempt(0))
        assertEquals(2000L, strategy.delayForAttempt(1))
        assertEquals(4000L, strategy.delayForAttempt(2))
        assertEquals(8000L, strategy.delayForAttempt(3))
        // Capped at maxMs
        assertEquals(10_000L, strategy.delayForAttempt(4))
    }

    @Test
    fun noneBackoffIsZero() {
        val strategy = BackoffStrategy.None
        assertEquals(0L, strategy.delayForAttempt(0))
        assertEquals(0L, strategy.delayForAttempt(10))
    }
}
