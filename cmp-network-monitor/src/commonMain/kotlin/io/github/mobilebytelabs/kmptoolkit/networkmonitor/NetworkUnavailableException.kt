package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Thrown when a network-dependent operation cannot proceed due to lack of connectivity.
 *
 * Extends [RuntimeException] (not IOException) because `java.io.IOException` is JVM-only
 * and unavailable in KMP commonMain. Consumer code that checks for network errors should
 * check for both `IOException` and `NetworkUnavailableException` explicitly.
 */
class NetworkUnavailableException(
    message: String = "No stable network connection available",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
