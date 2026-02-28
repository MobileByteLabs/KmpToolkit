package com.mobilebytelabs.kmptoolkit.appupdate.test

import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo
import com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver

/**
 * Test implementation of VersionResolver for unit testing.
 * Allows configurable responses for testing different scenarios.
 */
class TestVersionResolver(
    private val versionInfo: VersionInfo = VersionInfo(
        version = "1.0.0",
        updateType = UpdateType.FLEXIBLE,
    ),
    private val shouldThrow: Boolean = false,
    private val throwable: Throwable = RuntimeException("Test error"),
) : VersionResolver {

    var resolveCallCount = 0
        private set

    override suspend fun resolve(): VersionInfo {
        resolveCallCount++
        if (shouldThrow) {
            throw throwable
        }
        return versionInfo
    }

    companion object {
        /**
         * Creates a resolver that always returns the specified version.
         */
        fun withVersion(version: String, updateType: UpdateType = UpdateType.FLEXIBLE): TestVersionResolver =
            TestVersionResolver(
                versionInfo = VersionInfo(version = version, updateType = updateType),
            )

        /**
         * Creates a resolver that always throws an exception.
         */
        fun throwing(message: String = "Test error"): TestVersionResolver = TestVersionResolver(
            shouldThrow = true,
            throwable = RuntimeException(message),
        )

        /**
         * Creates a resolver with full version info.
         */
        fun withFullInfo(
            version: String,
            updateType: UpdateType,
            releaseNotes: String? = null,
            downloadUrl: String? = null,
            storeUrl: String? = null,
        ): TestVersionResolver = TestVersionResolver(
            versionInfo = VersionInfo(
                version = version,
                updateType = updateType,
                releaseNotes = releaseNotes,
                downloadUrl = downloadUrl,
                storeUrl = storeUrl,
            ),
        )
    }
}
