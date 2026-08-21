/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MeasurementProtocolAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The Measurement-Protocol tier's `provideAnalyticsHelper()` must auto-wire the
 * MP helper from the stashed [FirebaseConfig] when a [MpConfig] is present, and
 * fall back to [NoOpAnalyticsHelper] otherwise. Runs on JVM (the MP tier).
 */
class AnalyticsTierRoutingTest {

    @AfterTest
    fun reset() {
        FirebaseRuntime.config = null
    }

    @Test
    fun mp_helper_when_measurement_protocol_present() {
        FirebaseRuntime.config = FirebaseConfig(
            measurementProtocol = MpConfig(measurementId = "G-TEST", apiSecret = "secret"),
        )
        assertTrue(provideAnalyticsHelper() is MeasurementProtocolAnalyticsHelper)
    }

    @Test
    fun noop_when_measurement_protocol_absent() {
        FirebaseRuntime.config = FirebaseConfig()
        assertSame(NoOpAnalyticsHelper, provideAnalyticsHelper())
    }

    @Test
    fun noop_when_no_config_at_all() {
        FirebaseRuntime.config = null
        assertSame(NoOpAnalyticsHelper, provideAnalyticsHelper())
    }
}
