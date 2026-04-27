@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.mobilebytelabs.kmptoolkit.openurl

import platform.windows.SW_SHOWNORMAL
import platform.windows.ShellExecuteW

actual fun openUrl(url: String): Boolean = try {
    // ShellExecuteW opens the URL with the system default handler.
    // Return value > 32 means success (per Win32 docs).
    val result = ShellExecuteW(
        hwnd = null,
        lpOperation = "open",
        lpFile = url,
        lpParameters = null,
        lpDirectory = null,
        nShowCmd = SW_SHOWNORMAL,
    )
    // HINSTANCE > 32 means success per Win32 docs; rawValue gives the pointer as Long
    (result?.rawValue?.toLong() ?: 0L) > 32L
} catch (_: Throwable) {
    false
}

actual fun openInBrowser(url: String): Boolean = openUrl(url)

actual fun openWithApp(url: String, appHint: AppHint): OpenUrlResult = try {
    if (openUrl(url)) OpenUrlResult.Success else OpenUrlResult.NoHandler
} catch (e: Throwable) {
    OpenUrlResult.Error(e.message ?: "Windows error opening URL")
}

actual fun canOpen(url: String): Boolean = true // ShellExecuteW handles most schemes on Windows
