package com.mobilebytelabs.kmptoolkit.deeplink

/**
 * Linux entry point for deep link handling.
 *
 * Parses command-line arguments for a URI and forwards it to [DeepLinkHandler].
 * The OS delivers the URI via the `Exec=` line in your `.desktop` file:
 *
 * ```ini
 * [Desktop Entry]
 * Name=My App
 * Exec=/opt/myapp/myapp %u
 * MimeType=x-scheme-handler/myapp;
 * ```
 *
 * Register the handler:
 * ```bash
 * xdg-mime default myapp.desktop x-scheme-handler/myapp
 * update-desktop-database ~/.local/share/applications
 * ```
 *
 * See `docs/DESKTOP.md` for full setup instructions.
 *
 * **Note:** This one call is the irreducible minimum on Linux. The OS delivers the URI
 * as a CLI argument; automatic interception without consumer `main()` wiring is not possible.
 */
fun DeepLinkHandler.handleLaunchArgs(args: Array<String>) {
    args.firstOrNull { it.contains("://") || it.startsWith("https://") || it.startsWith("http://") }
        ?.let { handle(it) }
}
