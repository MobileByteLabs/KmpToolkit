/*
 * Copyright 2026 MobileByteLabs · Apache 2.0
 */
package com.mobilebytelabs.kmptoolkit.samples.toolkit.features.openurl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.openurl.open
import com.mobilebytelabs.kmptoolkit.openurl.openInBrowser
import com.mobilebytelabs.kmptoolkit.samples.toolkit.features._shared.DemoIntro

@Composable
fun OpenUrlDemoScreen(onStatus: (String) -> Unit) {
    DemoIntro("Single extension on String — `.open()` routes by scheme (http→browser, mailto→mail, tel→dialer, sms→messaging).")

    Button(
        onClick = {
            val ok = "https://github.com/MobileByteLabs/KmpToolkit".openInBrowser()
            onStatus("openInBrowser → $ok")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Open GitHub in browser") }

    Button(
        onClick = {
            val ok = "mailto:support@mobilebytelabs.io?subject=Hello".open()
            onStatus("mailto → $ok")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Open email composer (mailto:)") }

    Button(
        onClick = {
            val ok = "tel:+15551234567".open()
            onStatus("tel → $ok")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Open dialer (tel:)") }

    Button(
        onClick = {
            val ok = "sms:+15551234567?body=Hi".open()
            onStatus("sms → $ok")
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Open SMS composer (sms:)") }
}
