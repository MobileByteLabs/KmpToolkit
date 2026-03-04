package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * Linux implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "linux"
}
