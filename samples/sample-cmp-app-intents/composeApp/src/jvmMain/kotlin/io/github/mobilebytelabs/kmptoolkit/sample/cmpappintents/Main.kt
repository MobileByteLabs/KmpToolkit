package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "sample-cmp-app-intents",
    ) {
        App()
    }
}
