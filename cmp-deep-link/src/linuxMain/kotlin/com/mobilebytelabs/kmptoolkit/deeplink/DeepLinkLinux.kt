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
 */
fun DeepLinkHandler.handleLaunchArgs(args: Array<String>) {
    args.firstOrNull { it.contains("://") || it.startsWith("https://") || it.startsWith("http://") }
        ?.let { handle(it) }
}
