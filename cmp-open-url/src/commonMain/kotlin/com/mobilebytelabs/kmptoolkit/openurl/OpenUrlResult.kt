package com.mobilebytelabs.kmptoolkit.openurl

/**
 * Result of an [openWithApp] call. Provides richer error information than the
 * simple [Boolean] returned by [openUrl] and [openInBrowser].
 */
sealed class OpenUrlResult {

    /** The URL was successfully handed off to a platform handler. */
    object Success : OpenUrlResult()

    /**
     * No installed application or system handler could open the URL.
     * This is the expected result on headless/display-less platforms
     * (tvOS, watchOS, wasmWasi) and when the requested [AppHint.Custom]
     * package is not installed.
     */
    object NoHandler : OpenUrlResult()

    /**
     * An unexpected error occurred while attempting to open the URL.
     * @param message A human-readable description of the error.
     */
    data class Error(val message: String) : OpenUrlResult()
}
