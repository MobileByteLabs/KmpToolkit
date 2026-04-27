package com.mobilebytelabs.kmptoolkit.openurl

/**
 * Open this URL with the platform's default handler.
 *
 * ```kotlin
 * "https://github.com/MobileByteLabs".open()
 * "myapp://deep-link/home".open()
 * ```
 *
 * @return true if the platform accepted the URL, false otherwise. Never throws.
 */
fun String.open(): Boolean = openUrl(this)

/**
 * Open this URL in the system browser, bypassing app-association rules.
 *
 * ```kotlin
 * "https://accounts.google.com/signin".openInBrowser()
 * ```
 *
 * @return true if the browser was launched, false otherwise. Never throws.
 */
fun String.openInBrowser(): Boolean = openInBrowser(this)

/**
 * Open this URL with a preferred app category hint.
 *
 * ```kotlin
 * "mailto:hi@example.com".openWith(AppHint.EMAIL)
 * "geo:37.7749,-122.4194".openWith(AppHint.MAPS)
 * "tel:+15550001234".openWith(AppHint.PHONE)
 * "sms:+15550001234".openWith(AppHint.SMS)
 * ```
 *
 * @return [OpenUrlResult.Success], [OpenUrlResult.NoHandler], or [OpenUrlResult.Error]. Never throws.
 */
fun String.openWith(hint: AppHint = AppHint.DEFAULT): OpenUrlResult = openWithApp(this, hint)

/**
 * Check whether the platform can handle this URL without actually opening it.
 *
 * ```kotlin
 * if ("myapp://home".canOpen()) {
 *     "myapp://home".open()
 * }
 * ```
 *
 * @return true if a handler exists, false otherwise. Never throws.
 */
fun String.canOpen(): Boolean = canOpen(this)
