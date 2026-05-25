/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.interappcomms

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Inter-App Comms Sample",
        state = rememberWindowState(width = 700.dp, height = 800.dp),
    ) {
        SampleInterAppCommsApp()
    }
}
