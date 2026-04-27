package com.mobilebytelabs.kmptoolkit.openurl

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String): Boolean = try {
    val uri = URI(url)
    when {
        Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) -> {
            Desktop.getDesktop().browse(uri)
            true
        }

        isLinux() -> xdgOpen(url)

        else -> false
    }
} catch (_: Exception) {
    false
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    if (openUrl(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
} catch (e: Exception) {
    OpenUrlResult.Error(e.message ?: "JVM error opening URL")
}

actual fun canOpen(url: String): Boolean = try {
    when {
        Desktop.isDesktopSupported() -> true

        isLinux() -> true

        // assume xdg-open available
        else -> false
    }
} catch (_: Exception) {
    false
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private fun isLinux(): Boolean = System.getProperty("os.name")?.contains("linux", ignoreCase = true) == true

private fun xdgOpen(url: String): Boolean = try {
    Runtime.getRuntime().exec(arrayOf("xdg-open", url))
    true
} catch (_: Exception) {
    false
}
