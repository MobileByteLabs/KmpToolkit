package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

/**
 * watchOS implementation of platform detector.
 */
internal actual object PlatformDetector {
    actual fun detect(): String = "watchos"
}
