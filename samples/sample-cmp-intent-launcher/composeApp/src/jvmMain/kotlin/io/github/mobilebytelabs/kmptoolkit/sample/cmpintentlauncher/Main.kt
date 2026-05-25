package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "sample-cmp-intent-launcher",
    ) {
        App()
    }
}
