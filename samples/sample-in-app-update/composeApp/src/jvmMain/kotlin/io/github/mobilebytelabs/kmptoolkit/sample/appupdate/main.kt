package io.github.mobilebytelabs.kmptoolkit.sample.appupdate

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "In-App Update Sample"
    ) {
        App()
    }
}
