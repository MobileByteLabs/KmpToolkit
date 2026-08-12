/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.firebaseanalytics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.Param
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.StubAnalyticsHelper
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard

@Composable
fun FirebaseAnalyticsDemoScreen(onStatus: (String) -> Unit) {
    val analytics: AnalyticsHelper = StubAnalyticsHelper()

    DemoIntro("Analytics interface with three impls: GitLive (Android/iOS), Measurement Protocol HTTP (Desktop/Web/Native), and Stub (testing). Tap a button to log via the stub.")

    Button(
        onClick = {
            analytics.logEvent(
                AnalyticsEvent(
                    type = "demo_button_clicked",
                    extras = listOf(Param("button_id", "log_event_demo")),
                ),
            )
            onStatus("Logged 'demo_button_clicked' to stub")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Log AnalyticsEvent (stub)") }

    SetupRequiredCard(
        title = "Wire a real backend for production",
        explanation = "StubAnalyticsHelper is in-process — events never leave the app. Choose GitLive (mobile) or Measurement Protocol (desktop/web) for actual delivery.",
        setupSteps = listOf(
            "Mobile: createGitLiveAnalyticsHelper() — needs Firebase config",
            "Desktop/Web/Native: MeasurementProtocolAnalyticsHelper(measurementId, apiSecret)",
            "Register in DI: single AnalyticsHelper instance for the whole app",
        ),
    )
}
