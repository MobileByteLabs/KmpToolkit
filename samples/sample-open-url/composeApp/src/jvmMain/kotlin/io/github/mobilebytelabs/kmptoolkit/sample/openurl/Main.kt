package io.github.mobilebytelabs.kmptoolkit.sample.openurl

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Open URL — KMP Sample",
    ) {
        App()
    }
}
