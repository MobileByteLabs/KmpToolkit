/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.firebaseanalytics

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseConfig
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseKit
import io.github.mobilebytelabs.kmptoolkit.firebase.FirebaseOptions
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsEvent
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.AnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.Param
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.StubAnalyticsHelper
import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.mp.MpConfig
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
        title = "One commonMain init for production",
        explanation = "One call configures every platform — no google-services.json / GoogleService-Info.plist / Swift configure() line. See exampleCommonMainInit() below.",
        setupSteps = listOf(
            "commonMain: FirebaseKit.initialize(FirebaseConfig(android=…, apple=…, web=…, measurementProtocol=…))",
            "Native (Android/iOS/macOS/tvOS/JS): GitLive initializes Firebase from your keys",
            "Fallback (JVM/desktop/web-wasm/native): Measurement Protocol via the same MpConfig",
        ),
    )
}

/**
 * Reference: the entire Firebase setup for every platform is this one commonMain
 * call. Firebase apiKey/appId/projectId are client identifiers (safe in source);
 * the Measurement-Protocol apiSecret is loaded from a secrets store at runtime.
 */
fun exampleCommonMainInit(mpApiSecret: String) {
    FirebaseKit.initialize(
        FirebaseConfig(
            android = FirebaseOptions(applicationId = "1:123:android:abc", apiKey = "AIza…", projectId = "demo"),
            apple = FirebaseOptions(applicationId = "1:123:ios:def", apiKey = "AIza…", gcmSenderId = "123"),
            web = FirebaseOptions(applicationId = "1:123:web:ghi", apiKey = "AIza…", authDomain = "demo.firebaseapp.com"),
            measurementProtocol = MpConfig(measurementId = "G-DEMO", apiSecret = mpApiSecret),
        ),
    )
}
