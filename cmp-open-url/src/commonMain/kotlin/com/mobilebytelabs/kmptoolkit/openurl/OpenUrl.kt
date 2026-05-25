package com.mobilebytelabs.kmptoolkit.openurl

/**
 * Opens [url] using the platform's default handler.
 *
 * For `https://` URLs this is typically the system browser.
 * For `mailto:`, `tel:`, `sms:`, `geo:` etc. the OS routes to the appropriate app.
 * For unrecognised schemes the call returns `false` without throwing.
 *
 * @param url A well-formed URL or URI (encoding is the caller's responsibility).
 * @return `true` if the platform accepted the URL; `false` if no handler was found
 *         or an error occurred. Never throws.
 */
expect fun openUrl(url: String): Boolean

/**
 * Opens [url] explicitly in the system web browser, bypassing any app-association rules.
 *
 * Useful when you want to guarantee a browser opens (e.g. OAuth redirect) rather than
 * letting the OS delegate to an installed app that claims the scheme.
 *
 * @param url A well-formed HTTP/HTTPS URL.
 * @return `true` if the browser was launched; `false` otherwise. Never throws.
 */
expect fun openInBrowser(url: String): Boolean

/**
 * Opens [url] with a preference for the app category described by [appHint].
 *
 * Per-platform behaviour:
 * - **Android** — routes via `Intent.ACTION_VIEW` with action overrides per hint
 *   (`EMAIL` → `ACTION_SENDTO`, `PHONE` → `ACTION_DIAL`, `Custom(pkg)` → `setPackage`, etc.).
 * - **iOS / macOS** — rewrites the URL per hint BEFORE calling `UIApplication.openURL` /
 *   `NSWorkspace.openURL` (Apple platforms route by URL scheme). `EMAIL` requires `mailto:`;
 *   `PHONE` requires `tel:` or numeric input; `SMS` requires `sms:` or numeric; `MAPS` accepts
 *   `maps:` / `geo:` / `https://maps.*` URLs. Incompatible (url, hint) combinations return
 *   [OpenUrlResult.Error] with a message. Before kmp-toolkit 3.2.13 the hint was silently
 *   ignored on Apple platforms (G6 parity bug; fixed per
 *   plan-layer/project-plans/.../inter-app-comms-suite/03-open-url-g6-fix.md).
 * - **`AppHint.Custom(packageName)` is ANDROID-ONLY** — on iOS / macOS / JVM / JS / wasmJs
 *   it falls back to [AppHint.DEFAULT] behaviour silently.
 *
 * Returns:
 * - [OpenUrlResult.Success] — platform handler accepted the URL.
 * - [OpenUrlResult.NoHandler] — no handler found (expected on tvOS, watchOS, wasmWasi;
 *   also when Apple finds no app registered for the scheme).
 * - [OpenUrlResult.Error] — an unexpected platform error occurred, OR the hint cannot
 *   be satisfied for the given URL (e.g. `AppHint.PHONE` with a non-numeric HTTPS URL on iOS).
 *
 * @param url A well-formed URL or URI.
 * @param appHint Preferred app category. Defaults to [AppHint.DEFAULT].
 * @return An [OpenUrlResult] describing the outcome. Never throws.
 */
expect fun openWithApp(url: String, appHint: AppHint = AppHint.DEFAULT): OpenUrlResult

/**
 * Returns `true` if this platform can handle [url] without actually opening it.
 *
 * Useful for hiding buttons or showing fallback UI before attempting to open.
 * On platforms with no display (wasmWasi) this always returns `false`.
 * On JS browser targets this always returns `true`.
 *
 * @param url A well-formed URL or URI.
 * @return `true` if a handler exists; `false` if not or on error. Never throws.
 */
expect fun canOpen(url: String): Boolean
