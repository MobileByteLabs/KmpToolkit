package io.github.mobilebytelabs.kmptoolkit.sample.networkmonitor

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Network Monitor Sample",
    ) {
        App()
    }
}
