package com.mobilebytelabs.kmptoolkit.openurl

/**
 * Hint to [openWithApp] about which platform app category should handle the URL.
 *
 * On platforms that don't support a specific hint, the library silently falls back
 * to [DEFAULT] behavior — it never throws.
 */
sealed class AppHint {

    /** Let the platform choose the best handler based on the URL scheme (default). */
    object DEFAULT : AppHint()

    /** Force the URL to open in the system web browser. */
    object BROWSER : AppHint()

    /**
     * Open in the default email client (Gmail, Apple Mail, Thunderbird, etc.).
     * Typically used with `mailto:` URLs.
     */
    object EMAIL : AppHint()

    /**
     * Open in the default maps application (Google Maps, Apple Maps, etc.).
     * Typically used with `geo://` or `https://maps.google.com` URLs.
     */
    object MAPS : AppHint()

    /**
     * Open in the phone dialer.
     * Typically used with `tel:` URLs.
     */
    object PHONE : AppHint()

    /**
     * Open in the default SMS/messaging application.
     * Typically used with `sms:` URLs.
     */
    object SMS : AppHint()

    /**
     * Android-only: explicitly target an app by its package name.
     *
     * **On iOS / macOS / JVM / JS / wasmJs this falls back to [DEFAULT] behaviour silently**
     * (the host platform has no concept of "open in this specific package"). Callers on
     * non-Android platforms get the platform-default URL handler.
     *
     * If the specified package is not installed on Android, the library retries
     * with [DEFAULT] before returning [OpenUrlResult.NoHandler].
     *
     * @param packageName The Android application package (e.g. `"com.google.android.gm"`)
     */
    data class Custom(val packageName: String) : AppHint()
}
