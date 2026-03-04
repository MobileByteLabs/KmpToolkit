package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo
import com.mobilebytelabs.kmptoolkit.appupdate.VersionResolver
import com.mobilebytelabs.kmptoolkit.appupdate.test.TestVersionResolver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VersionResolverTest {

    @Test
    fun resolveReturnsVersionInfo() = runTest {
        val resolver = TestVersionResolver.withVersion("1.2.3")
        val result = resolver.resolve()
        assertEquals("1.2.3", result.version)
    }

    @Test
    fun resolveWithUpdateType() = runTest {
        val resolver = TestVersionResolver.withVersion("2.0.0", UpdateType.IMMEDIATE)
        val result = resolver.resolve()
        assertEquals(UpdateType.IMMEDIATE, result.updateType)
    }

    @Test
    fun resolveWithFullInfo() = runTest {
        val resolver = TestVersionResolver.withFullInfo(
            version = "3.0.0",
            updateType = UpdateType.FLEXIBLE,
            releaseNotes = "New features",
            downloadUrl = "https://example.com/app.apk",
            storeUrl = "https://play.google.com",
        )
        val result = resolver.resolve()

        assertEquals("3.0.0", result.version)
        assertEquals(UpdateType.FLEXIBLE, result.updateType)
        assertEquals("New features", result.releaseNotes)
        assertEquals("https://example.com/app.apk", result.downloadUrl)
        assertEquals("https://play.google.com", result.storeUrl)
    }

    @Test
    fun resolveThrowsException() = runTest {
        val resolver = TestVersionResolver.throwing("Network error")

        var exceptionThrown = false
        try {
            resolver.resolve()
        } catch (e: RuntimeException) {
            exceptionThrown = true
            assertEquals("Network error", e.message)
        }
        assertTrue(exceptionThrown, "Expected RuntimeException to be thrown")
    }

    @Test
    fun resolveCallCountTracked() = runTest {
        val resolver = TestVersionResolver.withVersion("1.0.0")

        assertEquals(0, resolver.resolveCallCount)
        resolver.resolve()
        assertEquals(1, resolver.resolveCallCount)
        resolver.resolve()
        assertEquals(2, resolver.resolveCallCount)
    }

    @Test
    fun customResolverImplementation() = runTest {
        // Test that the interface can be implemented
        val customResolver = object : VersionResolver {
            override suspend fun resolve(): VersionInfo = VersionInfo(
                version = "custom-1.0.0",
                updateType = UpdateType.NONE,
            )
        }

        val result = customResolver.resolve()
        assertEquals("custom-1.0.0", result.version)
        assertEquals(UpdateType.NONE, result.updateType)
    }

    @Test
    fun defaultUpdateTypeFlexible() = runTest {
        val resolver = TestVersionResolver(
            versionInfo = VersionInfo(version = "1.0.0"),
        )
        val result = resolver.resolve()
        assertEquals(UpdateType.FLEXIBLE, result.updateType)
    }

    @Test
    fun metadataAccessible() = runTest {
        val resolver = TestVersionResolver(
            versionInfo = VersionInfo(
                version = "1.0.0",
                metadata = mapOf("key" to "value"),
            ),
        )
        val result = resolver.resolve()
        assertEquals("value", result.metadata["key"])
    }

    @Test
    fun testVersionResolverNotNull() {
        val resolver = TestVersionResolver.withVersion("1.0.0")
        assertNotNull(resolver)
    }

    @Test
    fun testVersionResolverIsVersionResolver() {
        val resolver: VersionResolver = TestVersionResolver.withVersion("1.0.0")
        assertTrue(resolver is TestVersionResolver)
    }
}
