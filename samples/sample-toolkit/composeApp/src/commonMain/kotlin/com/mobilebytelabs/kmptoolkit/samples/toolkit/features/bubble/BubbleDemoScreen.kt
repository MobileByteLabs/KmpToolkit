/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.bubble

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.SetupRequiredCard

@Composable
fun BubbleDemoScreen(onStatus: (String) -> Unit) {
    LaunchedEffect(Unit) { onStatus("Setup required for interactive demo") }
    DemoIntro("Floating UI bubbles + system notifications. Platform-specific APIs — Android (notification-bubbles), iOS (UNUserNotificationCenter), Desktop/Web (SystemTray / Notification API).")
    SetupRequiredCard(
        title = "Platform-specific setup",
        explanation = "cmp-bubble's primary surface lives in platform sources (notification permissions, channels, manifest entries). Run the per-module sample-bubble Android app for the full demo, or follow setup docs to wire bubbles into your project.",
        setupSteps = listOf(
            "Android: add POST_NOTIFICATIONS permission + notification channel",
            "iOS: request notification authorization at app launch",
            "See docs/bubble/SETUP.md for full per-platform integration",
        ),
    )
}
