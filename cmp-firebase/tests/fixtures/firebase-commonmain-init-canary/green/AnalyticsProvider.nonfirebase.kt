// GREEN fixture — the FIX: the Measurement-Protocol tier auto-wires the MP helper
// from the stashed FirebaseConfig when a MpConfig is present, else NoOp. One
// commonMain FirebaseConfig drives both tiers; events flow everywhere keys exist.
package io.github.mobilebytelabs.kmptoolkit.firebase.analytics

import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseRuntime
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MeasurementProtocolAnalyticsHelper

actual fun provideAnalyticsHelper(): AnalyticsHelper = FirebaseRuntime.config?.measurementProtocol
    ?.let { MeasurementProtocolAnalyticsHelper(config = it, settings = InMemorySettings()) }
    ?: NoOpAnalyticsHelper
