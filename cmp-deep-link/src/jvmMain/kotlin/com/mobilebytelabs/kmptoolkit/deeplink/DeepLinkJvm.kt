package com.mobilebytelabs.kmptoolkit.deeplink

/**
 * JVM Desktop entry point for deep link handling.
 *
 * Call [DeepLinkHandler.handleLaunchArgs] from your application's `main` function
 * to process a URI passed as a command-line argument on startup.
 *
 * ## Usage
 *
 * ```kotlin
 * fun main(args: Array<String>) {
 *     DeepLinkHandler.handleLaunchArgs(args)
 *     // Start your Compose Desktop window...
 * }
 * ```
 *
 * See `docs/DESKTOP.md` for:
 * - Linux `.desktop` Exec= configuration
 * - Windows registry / AppxManifest setup
 * - macOS CFBundleURLTypes plist entries
 */
fun DeepLinkHandler.handleLaunchArgs(args: Array<String>) {
    args.firstOrNull { it.contains("://") || it.startsWith("https://") || it.startsWith("http://") }
        ?.let { handle(it) }
}
