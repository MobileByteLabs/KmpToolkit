package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.testing.FakeNetworkMonitor
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StressTest {

    @Test
    fun rapidToggle1000Times() = runTest {
        val monitor = FakeNetworkMonitor()
        repeat(1000) { i ->
            monitor.setOnline(i % 2 == 0)
        }
        // 1000 toggles: 0..999, last i=999, 999%2=1, setOnline(false)
        assertFalse(monitor.isOnline.value)
    }

    @Test
    fun rapidTypeChanges() = runTest {
        val monitor = FakeNetworkMonitor()
        val types = listOf(
            NetworkType.WiFi,
            NetworkType.Cellular,
            NetworkType.FiveG,
            NetworkType.Ethernet,
            NetworkType.VPN,
            NetworkType.Bluetooth,
            NetworkType.Unknown,
        )

        repeat(500) { i ->
            val type = types[i % types.size]
            monitor.setNetworkStatus(
                NetworkStatus.Available(
                    NetworkInfo(type = type, isMetered = type == NetworkType.Cellular),
                ),
            )
        }

        // Should be stable at the last type
        val lastType = types[499 % types.size]
        val status = monitor.networkStatus.value
        assertTrue(status is NetworkStatus.Available)
        assertEquals(lastType, (status as NetworkStatus.Available).info.type)
    }

    @Test
    fun qualityComputationUnderLoad() {
        val types = listOf(
            NetworkType.WiFi,
            NetworkType.Cellular,
            NetworkType.FiveG,
            NetworkType.Ethernet,
            NetworkType.VPN,
            NetworkType.Bluetooth,
            NetworkType.Unknown,
        )
        val bandwidths = listOf(-1, 500, 1_000, 5_000, 10_000, 50_000, 100_000, 200_000)
        val meteredOptions = listOf(true, false)

        // Test all combinations — quality derivation should never crash
        for (type in types) {
            for (bw in bandwidths) {
                for (metered in meteredOptions) {
                    val info = NetworkInfo(
                        type = type,
                        isMetered = metered,
                        downstreamBandwidthKbps = bw,
                    )
                    val quality = info.toQuality()
                    // Quality should always be one of the valid values
                    assertTrue(quality in NetworkQuality.entries)
                }
            }
        }
    }

    @Test
    fun constraintCheckAllCombinations() {
        val constraints = listOf(
            NetworkConstraint.RequiresConnectivity,
            NetworkConstraint.RequiresWiFi,
            NetworkConstraint.RequiresUnmetered,
            NetworkConstraint.RequiresMinBandwidth(5_000),
        )

        val statuses = listOf(
            NetworkStatus.Unavailable,
            NetworkStatus.Available(NetworkInfo()),
            NetworkStatus.Available(
                NetworkInfo(type = NetworkType.WiFi, isMetered = false, downstreamBandwidthKbps = 10_000),
            ),
            NetworkStatus.Available(
                NetworkInfo(type = NetworkType.Cellular, isMetered = true, downstreamBandwidthKbps = 1_000),
            ),
            NetworkStatus.CaptivePortal(NetworkInfo(type = NetworkType.WiFi)),
        )

        // No constraint check should crash
        for (status in statuses) {
            for (constraint in constraints) {
                constraint.isSatisfiedBy(status)
            }
        }
    }

    @Test
    fun retryPolicyBuilderStress() {
        repeat(100) { i ->
            val policy = RetryPolicy {
                maxAttempts = i + 1
                backoff = when (i % 3) {
                    0 -> BackoffStrategy.Fixed((i * 100).toLong())

                    1 -> BackoffStrategy.Exponential(
                        baseMs = (i * 10).toLong(),
                        maxMs = (i * 1000).toLong(),
                    )

                    else -> BackoffStrategy.None
                }
                timeoutMs = (i * 1000).toLong()
            }
            assertEquals(i + 1, policy.maxAttempts)
        }
    }
}
