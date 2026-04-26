package com.mobilebytelabs.kmptoolkit.deeplink

/**
 * Windows (MinGW) entry point for deep link handling.
 *
 * Parses command-line arguments for a URI and forwards it to [DeepLinkHandler].
 * Windows delivers the URI via registry / AppxManifest:
 *
 * **Registry (classic Win32):**
 * ```
 * HKEY_CLASSES_ROOT\myapp\shell\open\command
 *   (Default) = "C:\Path\myapp.exe" "%1"
 * ```
 *
 * **MSIX / AppxManifest:**
 * ```xml
 * <uap:Protocol Name="myapp">
 *   <uap:DisplayName>My App</uap:DisplayName>
 * </uap:Protocol>
 * ```
 *
 * See `docs/DESKTOP.md` for full setup instructions.
 */
fun DeepLinkHandler.handleLaunchArgs(args: Array<String>) {
    args.firstOrNull { it.contains("://") || it.startsWith("https://") || it.startsWith("http://") }
        ?.let { handle(it) }
}
