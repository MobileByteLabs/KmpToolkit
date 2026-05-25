package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "sample-cmp-share",
    ) {
        App()
    }
}
