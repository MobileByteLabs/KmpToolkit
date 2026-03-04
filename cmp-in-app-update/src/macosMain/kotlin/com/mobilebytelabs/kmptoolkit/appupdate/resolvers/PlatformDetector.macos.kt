package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * macOS implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "macos"
}
