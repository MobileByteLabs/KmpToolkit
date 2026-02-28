package com.mobilebytelabs.kmptoolkit.appupdate

import com.mobilebytelabs.kmptoolkit.appupdate.resolvers.GitHubResolver
import com.mobilebytelabs.kmptoolkit.appupdate.resolvers.SupabaseResolver
import com.mobilebytelabs.kmptoolkit.appupdate.test.TestVersionResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppUpdateConfigBuilderTest {

    // ========================================================================
    // Platform-Specific Builder Methods
    // ========================================================================

    @Test
    fun androidSetsPackageName() {
        val config = AppUpdateConfig.builder()
            .android(packageName = "com.example.app")
            .build()
        assertEquals("com.example.app", config.packageName)
    }

    @Test
    fun androidEnablesPlatform() {
        val config = AppUpdateConfig.builder()
            .disableAndroid()
            .android(packageName = "com.example.app")
            .build()
        assertTrue(config.androidEnabled)
    }

    @Test
    fun iosSetsAppStoreId() {
        val config = AppUpdateConfig.builder()
            .ios(appStoreId = "123456789")
            .build()
        assertEquals("123456789", config.iosAppStoreId)
    }

    @Test
    fun iosEnablesPlatform() {
        val config = AppUpdateConfig.builder()
            .disableIos()
            .ios(appStoreId = "123456789")
            .build()
        assertTrue(config.iosEnabled)
    }

    @Test
    fun macosSetsAppStoreId() {
        val config = AppUpdateConfig.builder()
            .macos(appStoreId = "987654321")
            .build()
        assertEquals("987654321", config.macosAppStoreId)
    }

    @Test
    fun macosSetsStoreUrl() {
        val config = AppUpdateConfig.builder()
            .macos(storeUrl = "https://apps.apple.com/app/my-app/id987654321")
            .build()
        assertEquals("https://apps.apple.com/app/my-app/id987654321", config.macosStoreUrl)
    }

    @Test
    fun jvmSetsDownloadUrl() {
        val config = AppUpdateConfig.builder()
            .jvm(downloadUrl = "https://example.com/app.jar")
            .build()
        assertEquals("https://example.com/app.jar", config.jvmDownloadUrl)
    }

    @Test
    fun jvmSetsVersionCheckUrl() {
        val config = AppUpdateConfig.builder()
            .jvm(versionCheckUrl = "https://api.example.com/version")
            .build()
        assertEquals("https://api.example.com/version", config.jvmVersionCheckUrl)
    }

    @Test
    fun linuxSetsStoreUrl() {
        val config = AppUpdateConfig.builder()
            .linux(storeUrl = "https://flathub.org/apps/com.example.app")
            .build()
        assertEquals("https://flathub.org/apps/com.example.app", config.linuxStoreUrl)
    }

    @Test
    fun linuxSetsVersionCheckUrl() {
        val config = AppUpdateConfig.builder()
            .linux(versionCheckUrl = "https://api.example.com/linux/version")
            .build()
        assertEquals("https://api.example.com/linux/version", config.linuxVersionCheckUrl)
    }

    @Test
    fun windowsSetsStoreUrl() {
        val config = AppUpdateConfig.builder()
            .windows(storeUrl = "ms-windows-store://pdp/?ProductId=XXX")
            .build()
        assertEquals("ms-windows-store://pdp/?ProductId=XXX", config.windowsStoreUrl)
    }

    @Test
    fun windowsSetsVersionCheckUrl() {
        val config = AppUpdateConfig.builder()
            .windows(versionCheckUrl = "https://api.example.com/windows/version")
            .build()
        assertEquals("https://api.example.com/windows/version", config.windowsVersionCheckUrl)
    }

    // ========================================================================
    // Resolver Builder Methods
    // ========================================================================

    @Test
    fun githubCreatesResolver() {
        val config = AppUpdateConfig.builder()
            .github(owner = "myorg", repo = "myapp")
            .build()
        assertNotNull(config.versionResolver)
        assertTrue(config.versionResolver is GitHubResolver)
    }

    @Test
    fun githubWithToken() {
        val config = AppUpdateConfig.builder()
            .github(owner = "myorg", repo = "myapp", token = "ghp_xxx")
            .build()
        assertNotNull(config.versionResolver)
        assertTrue(config.versionResolver is GitHubResolver)
    }

    @Test
    fun supabaseCreatesResolver() {
        val config = AppUpdateConfig.builder()
            .supabase(
                projectUrl = "https://project.supabase.co",
                anonKey = "eyJ...",
            )
            .build()
        assertNotNull(config.versionResolver)
        assertTrue(config.versionResolver is SupabaseResolver)
    }

    @Test
    fun supabaseWithCustomTable() {
        val config = AppUpdateConfig.builder()
            .supabase(
                projectUrl = "https://project.supabase.co",
                anonKey = "eyJ...",
                tableName = "versions",
            )
            .build()
        assertNotNull(config.versionResolver)
        assertTrue(config.versionResolver is SupabaseResolver)
    }

    @Test
    fun versionResolverSetsCustomResolver() {
        val customResolver = TestVersionResolver.withVersion("1.0.0")
        val config = AppUpdateConfig.builder()
            .versionResolver(customResolver)
            .build()
        assertEquals(customResolver, config.versionResolver)
    }

    // ========================================================================
    // Disable Methods
    // ========================================================================

    @Test
    fun disableAndroid() {
        val config = AppUpdateConfig.builder()
            .disableAndroid()
            .build()
        assertFalse(config.androidEnabled)
    }

    @Test
    fun disableIos() {
        val config = AppUpdateConfig.builder()
            .disableIos()
            .build()
        assertFalse(config.iosEnabled)
    }

    @Test
    fun disableMacos() {
        val config = AppUpdateConfig.builder()
            .disableMacos()
            .build()
        assertFalse(config.macosEnabled)
    }

    @Test
    fun disableJvm() {
        val config = AppUpdateConfig.builder()
            .disableJvm()
            .build()
        assertFalse(config.jvmEnabled)
    }

    @Test
    fun disableLinux() {
        val config = AppUpdateConfig.builder()
            .disableLinux()
            .build()
        assertFalse(config.linuxEnabled)
    }

    @Test
    fun disableWindows() {
        val config = AppUpdateConfig.builder()
            .disableWindows()
            .build()
        assertFalse(config.windowsEnabled)
    }

    @Test
    fun disableTvos() {
        val config = AppUpdateConfig.builder()
            .disableTvos()
            .build()
        assertFalse(config.tvosEnabled)
    }

    @Test
    fun disableWatchos() {
        val config = AppUpdateConfig.builder()
            .disableWatchos()
            .build()
        assertFalse(config.watchosEnabled)
    }

    // ========================================================================
    // Combined Configuration Tests
    // ========================================================================

    @Test
    fun multiPlatformConfig() {
        val config = AppUpdateConfig.builder()
            .android(packageName = "com.example.app")
            .ios(appStoreId = "123456789")
            .macos(appStoreId = "987654321")
            .jvm(downloadUrl = "https://example.com/app.jar")
            .github(owner = "myorg", repo = "myapp")
            .build()

        assertEquals("com.example.app", config.packageName)
        assertEquals("123456789", config.iosAppStoreId)
        assertEquals("987654321", config.macosAppStoreId)
        assertEquals("https://example.com/app.jar", config.jvmDownloadUrl)
        assertNotNull(config.versionResolver)
    }

    @Test
    fun mobileOnlyConfig() {
        val config = AppUpdateConfig.builder()
            .android(packageName = "com.example.app")
            .ios(appStoreId = "123456789")
            .mobileOnly()
            .build()

        assertTrue(config.androidEnabled)
        assertTrue(config.iosEnabled)
        assertFalse(config.jvmEnabled)
        assertFalse(config.linuxEnabled)
        assertFalse(config.windowsEnabled)
        assertFalse(config.macosEnabled)
    }

    @Test
    fun desktopOnlyConfig() {
        val config = AppUpdateConfig.builder()
            .jvm(downloadUrl = "https://example.com/app.jar")
            .linux(storeUrl = "https://flathub.org/apps/com.example")
            .windows(storeUrl = "ms-windows-store://pdp/?ProductId=XXX")
            .github(owner = "myorg", repo = "myapp")
            .desktopOnly()
            .build()

        assertFalse(config.androidEnabled)
        assertFalse(config.iosEnabled)
        assertTrue(config.jvmEnabled)
        assertTrue(config.linuxEnabled)
        assertTrue(config.windowsEnabled)
        assertNotNull(config.versionResolver)
    }

    @Test
    fun chainedMethodsReturnBuilder() {
        val config = AppUpdateConfig.builder()
            .android(packageName = "com.example")
            .ios(appStoreId = "123")
            .macos(appStoreId = "456")
            .jvm(downloadUrl = "https://example.com")
            .linux(storeUrl = "https://flathub.org")
            .windows(storeUrl = "ms-windows-store://")
            .github(owner = "org", repo = "repo")
            .countryCode("uk")
            .customVersionCheckUrl("https://api.example.com/version")
            .disableAndroid()
            .disableIos()
            .disableMacos()
            .disableJvm()
            .disableLinux()
            .disableWindows()
            .disableTvos()
            .disableWatchos()
            .build()

        assertNotNull(config)
    }

    // ========================================================================
    // Edge Cases
    // ========================================================================

    @Test
    fun emptyBuilderBuild() {
        val config = AppUpdateConfig.builder().build()
        // Should use defaults
        assertTrue(config.androidEnabled)
        assertTrue(config.iosEnabled)
        assertEquals("us", config.countryCode)
    }

    @Test
    fun resolverOverwrite() {
        val resolver1 = TestVersionResolver.withVersion("1.0.0")
        val resolver2 = TestVersionResolver.withVersion("2.0.0")

        val config = AppUpdateConfig.builder()
            .versionResolver(resolver1)
            .versionResolver(resolver2)
            .build()

        assertEquals(resolver2, config.versionResolver)
    }

    @Test
    fun githubOverwritesCustomResolver() {
        val customResolver = TestVersionResolver.withVersion("1.0.0")

        val config = AppUpdateConfig.builder()
            .versionResolver(customResolver)
            .github(owner = "org", repo = "repo")
            .build()

        assertTrue(config.versionResolver is GitHubResolver)
    }

    @Test
    fun supabaseOverwritesGithubResolver() {
        val config = AppUpdateConfig.builder()
            .github(owner = "org", repo = "repo")
            .supabase(projectUrl = "https://xxx.supabase.co", anonKey = "key")
            .build()

        assertTrue(config.versionResolver is SupabaseResolver)
    }

    @Test
    fun countryCodeSetsCorrectly() {
        val config = AppUpdateConfig.builder()
            .countryCode("gb")
            .build()
        assertEquals("gb", config.countryCode)
    }

    @Test
    fun nullPackageNameAccepted() {
        val config = AppUpdateConfig.builder()
            .android(packageName = null)
            .build()
        assertEquals(null, config.packageName)
        assertTrue(config.androidEnabled)
    }

    @Test
    fun nullAppStoreIdAccepted() {
        val config = AppUpdateConfig.builder()
            .ios(appStoreId = null)
            .build()
        assertEquals(null, config.iosAppStoreId)
        assertTrue(config.iosEnabled)
    }
}
