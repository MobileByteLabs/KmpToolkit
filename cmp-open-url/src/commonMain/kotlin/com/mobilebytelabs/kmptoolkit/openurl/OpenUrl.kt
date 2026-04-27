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
 * Provides richer feedback than [openUrl] via [OpenUrlResult]:
 * - [OpenUrlResult.Success] — platform handler accepted the URL.
 * - [OpenUrlResult.NoHandler] — no handler found (expected on tvOS, watchOS, wasmWasi).
 * - [OpenUrlResult.Error] — an unexpected platform error occurred.
 *
 * Hints that are not meaningful on the current platform are silently ignored and
 * the call falls back to [AppHint.DEFAULT] behaviour.
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
