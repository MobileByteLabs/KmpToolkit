package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkMonitorConfigTest {

    @Test
    fun defaultValues() {
        val config = NetworkMonitorConfig()
        assertEquals(3_000L, config.pollIntervalMs)
        assertEquals("https://clients3.google.com/generate_204", config.validationUrl)
        assertEquals(5_000L, config.validationTimeoutMs)
        assertEquals(ValidationStrategy.NativeThenHttp, config.validationStrategy)
        assertEquals(30_000L, config.backgroundPollIntervalMs)
        assertEquals(60_000L, config.maxValidationBackoffMs)
    }

    @Test
    fun builderDsl() {
        val config = NetworkMonitorConfig {
            pollIntervalMs = 10_000L
            validationStrategy = ValidationStrategy.NativeOnly
            backgroundPollIntervalMs = 60_000L
        }
        assertEquals(10_000L, config.pollIntervalMs)
        assertEquals(ValidationStrategy.NativeOnly, config.validationStrategy)
        assertEquals(60_000L, config.backgroundPollIntervalMs)
    }

    @Test
    fun customPollInterval() {
        val config = NetworkMonitorConfig(pollIntervalMs = 10_000L)
        assertEquals(10_000L, config.pollIntervalMs)
    }

    @Test
    fun customValidationUrl() {
        val config = NetworkMonitorConfig(validationUrl = "https://example.com/check")
        assertEquals("https://example.com/check", config.validationUrl)
    }

    @Test
    fun nativeOnlyStrategy() {
        val config = NetworkMonitorConfig(validationStrategy = ValidationStrategy.NativeOnly)
        assertEquals(ValidationStrategy.NativeOnly, config.validationStrategy)
    }

    @Test
    fun httpOnlyStrategy() {
        val config = NetworkMonitorConfig(validationStrategy = ValidationStrategy.HttpOnly)
        assertEquals(ValidationStrategy.HttpOnly, config.validationStrategy)
    }

    @Test
    fun validationStrategyEnumHasThreeValues() {
        assertEquals(3, ValidationStrategy.entries.size)
    }

    @Test
    fun copyPreservesUnchangedFields() {
        val original = NetworkMonitorConfig(
            pollIntervalMs = 5_000L,
            validationUrl = "https://custom.url",
        )
        val copied = original.copy(validationTimeoutMs = 10_000L)
        assertEquals(5_000L, copied.pollIntervalMs)
        assertEquals("https://custom.url", copied.validationUrl)
        assertEquals(10_000L, copied.validationTimeoutMs)
    }
}
