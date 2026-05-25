/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.mobilebytelabs.kmptoolkit.samples.interappcomms

import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

fun main() {
    ComposeViewport(document.body!!) {
        SampleInterAppCommsApp()
    }
}
