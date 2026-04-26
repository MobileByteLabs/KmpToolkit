package io.github.mobilebytelabs.kmptoolkit.sample.deeplink

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler
import com.mobilebytelabs.kmptoolkit.deeplink.handleLaunchArgs

fun main(args: Array<String>) {
    // Handle URI passed as CLI arg: e.g. ./sample-deep-link demo://open/product/1
    DeepLinkHandler.handleLaunchArgs(args)

    application {
        Window(onCloseRequest = ::exitApplication, title = "cmp-deep-link demo") {
            App()
        }
    }
}
