package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * Windows (mingw) implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "windows"
}
