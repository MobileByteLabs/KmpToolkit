package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * iOS implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "ios"
}
