package com.mobilebytelabs.kmptoolkit.appupdate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppUpdateConfigTest {

    // ========================================================================
    // Default Config Tests
    // ========================================================================

    @Test
    fun defaultConfigExists() {
        val config = AppUpdateConfig.Default
        assertEquals("us", config.countryCode)
    }

    @Test
    fun defaultConfigAndroidEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.androidEnabled)
    }

    @Test
    fun defaultConfigIosEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.iosEnabled)
    }

    @Test
    fun defaultConfigMacosEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.macosEnabled)
    }

    @Test
    fun defaultConfigJvmEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.jvmEnabled)
    }

    @Test
    fun defaultConfigLinuxEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.linuxEnabled)
    }

    @Test
    fun defaultConfigWindowsEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.windowsEnabled)
    }

    @Test
    fun defaultConfigTvosEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.tvosEnabled)
    }

    @Test
    fun defaultConfigWatchosEnabled() {
        val config = AppUpdateConfig.Default
        assertTrue(config.watchosEnabled)
    }

    @Test
    fun defaultConfigNoResolver() {
        val config = AppUpdateConfig.Default
        assertNull(config.versionResolver)
    }

    // ========================================================================
    // Builder Basic Tests
    // ========================================================================

    @Test
    fun builderCreatesEmptyConfig() {
        val config = AppUpdateConfig.builder().build()
        assertNull(config.packageName)
        assertNull(config.iosAppStoreId)
    }

    @Test
    fun builderAndroidSetsPackageName() {
        val config = AppUpdateConfig.builder()
            .android("com.example.app")
            .build()
        assertEquals("com.example.app", config.packageName)
    }

    @Test
    fun builderIosSetsAppStoreId() {
        val config = AppUpdateConfig.builder()
            .ios("123456789")
            .build()
        assertEquals("123456789", config.iosAppStoreId)
    }

    @Test
    fun builderMacosSetsAppStoreId() {
        val config = AppUpdateConfig.builder()
            .macos("987654321")
            .build()
        assertEquals("987654321", config.macosAppStoreId)
    }

    @Test
    fun builderCountryCode() {
        val config = AppUpdateConfig.builder()
            .countryCode("uk")
            .build()
        assertEquals("uk", config.countryCode)
    }

    @Test
    fun builderCustomVersionCheckUrl() {
        val config = AppUpdateConfig.builder()
            .customVersionCheckUrl("https://api.example.com/version")
            .build()
        assertEquals("https://api.example.com/version", config.customVersionCheckUrl)
    }

    // ========================================================================
    // Platform Store URL Tests
    // ========================================================================

    @Test
    fun builderWindowsStoreUrl() {
        val config = AppUpdateConfig.builder()
            .windows(storeUrl = "ms-windows-store://pdp/?ProductId=XXX")
            .build()
        assertEquals("ms-windows-store://pdp/?ProductId=XXX", config.windowsStoreUrl)
    }

    @Test
    fun builderLinuxStoreUrl() {
        val config = AppUpdateConfig.builder()
            .linux(storeUrl = "https://flathub.org/apps/com.example.app")
            .build()
        assertEquals("https://flathub.org/apps/com.example.app", config.linuxStoreUrl)
    }

    @Test
    fun builderJvmDownloadUrl() {
        val config = AppUpdateConfig.builder()
            .jvm(downloadUrl = "https://example.com/app.jar")
            .build()
        assertEquals("https://example.com/app.jar", config.jvmDownloadUrl)
    }

    // ========================================================================
    // Platform Disable Tests
    // ========================================================================

    @Test
    fun builderDisableAndroid() {
        val config = AppUpdateConfig.builder()
            .disableAndroid()
            .build()
        assertFalse(config.androidEnabled)
    }

    @Test
    fun builderDisableIos() {
        val config = AppUpdateConfig.builder()
            .disableIos()
            .build()
        assertFalse(config.iosEnabled)
    }

    @Test
    fun builderDisableMacos() {
        val config = AppUpdateConfig.builder()
            .disableMacos()
            .build()
        assertFalse(config.macosEnabled)
    }

    @Test
    fun builderDisableJvm() {
        val config = AppUpdateConfig.builder()
            .disableJvm()
            .build()
        assertFalse(config.jvmEnabled)
    }

    @Test
    fun builderDisableLinux() {
        val config = AppUpdateConfig.builder()
            .disableLinux()
            .build()
        assertFalse(config.linuxEnabled)
    }

    @Test
    fun builderDisableWindows() {
        val config = AppUpdateConfig.builder()
            .disableWindows()
            .build()
        assertFalse(config.windowsEnabled)
    }

    @Test
    fun builderDisableTvos() {
        val config = AppUpdateConfig.builder()
            .disableTvos()
            .build()
        assertFalse(config.tvosEnabled)
    }

    @Test
    fun builderDisableWatchos() {
        val config = AppUpdateConfig.builder()
            .disableWatchos()
            .build()
        assertFalse(config.watchosEnabled)
    }

    // ========================================================================
    // Copy/Data Class Tests
    // ========================================================================

    @Test
    fun copyModifiesValue() {
        val original = AppUpdateConfig.builder()
            .android("com.example.app")
            .build()
        val modified = original.copy(packageName = "com.example.app2")
        assertEquals("com.example.app2", modified.packageName)
        assertEquals("com.example.app", original.packageName)
    }

    @Test
    fun equalityWorks() {
        val config1 = AppUpdateConfig.builder()
            .android("com.example.app")
            .build()
        val config2 = AppUpdateConfig.builder()
            .android("com.example.app")
            .build()
        assertEquals(config1, config2)
    }

    // ========================================================================
    // Effective URL Tests
    // ========================================================================

    @Test
    fun effectiveJvmUrlReturnsJvmUrl() {
        val config = AppUpdateConfig.builder()
            .jvm(versionCheckUrl = "https://jvm.example.com/version")
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://jvm.example.com/version", config.getEffectiveJvmVersionCheckUrl())
    }

    @Test
    fun effectiveJvmUrlFallsBackToCustom() {
        val config = AppUpdateConfig.builder()
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://custom.example.com/version", config.getEffectiveJvmVersionCheckUrl())
    }

    @Test
    fun effectiveLinuxUrlReturnsLinuxUrl() {
        val config = AppUpdateConfig.builder()
            .linux(versionCheckUrl = "https://linux.example.com/version")
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://linux.example.com/version", config.getEffectiveLinuxVersionCheckUrl())
    }

    @Test
    fun effectiveLinuxUrlFallsBackToCustom() {
        val config = AppUpdateConfig.builder()
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://custom.example.com/version", config.getEffectiveLinuxVersionCheckUrl())
    }

    @Test
    fun effectiveWindowsUrlReturnsWindowsUrl() {
        val config = AppUpdateConfig.builder()
            .windows(versionCheckUrl = "https://windows.example.com/version")
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://windows.example.com/version", config.getEffectiveWindowsVersionCheckUrl())
    }

    @Test
    fun effectiveWindowsUrlFallsBackToCustom() {
        val config = AppUpdateConfig.builder()
            .customVersionCheckUrl("https://custom.example.com/version")
            .build()
        assertEquals("https://custom.example.com/version", config.getEffectiveWindowsVersionCheckUrl())
    }

    // ========================================================================
    // Preset Tests
    // ========================================================================

    @Test
    fun mobileOnlyPreset() {
        val config = AppUpdateConfig.builder()
            .mobileOnly()
            .build()
        assertTrue(config.androidEnabled)
        assertTrue(config.iosEnabled)
        assertFalse(config.macosEnabled)
        assertFalse(config.jvmEnabled)
        assertFalse(config.linuxEnabled)
        assertFalse(config.windowsEnabled)
    }

    @Test
    fun desktopOnlyPreset() {
        val config = AppUpdateConfig.builder()
            .desktopOnly()
            .build()
        assertFalse(config.androidEnabled)
        assertFalse(config.iosEnabled)
        assertTrue(config.macosEnabled)
        assertTrue(config.jvmEnabled)
        assertTrue(config.linuxEnabled)
        assertTrue(config.windowsEnabled)
    }

    @Test
    fun appleOnlyPreset() {
        val config = AppUpdateConfig.builder()
            .appleOnly()
            .build()
        assertFalse(config.androidEnabled)
        assertTrue(config.iosEnabled)
        assertTrue(config.macosEnabled)
        assertFalse(config.jvmEnabled)
        assertFalse(config.linuxEnabled)
        assertFalse(config.windowsEnabled)
    }

    @Test
    fun disableAllPlatformsPreset() {
        val config = AppUpdateConfig.builder()
            .disableAllPlatforms()
            .build()
        assertFalse(config.androidEnabled)
        assertFalse(config.iosEnabled)
        assertFalse(config.macosEnabled)
        assertFalse(config.jvmEnabled)
        assertFalse(config.linuxEnabled)
        assertFalse(config.windowsEnabled)
        assertFalse(config.tvosEnabled)
        assertFalse(config.watchosEnabled)
    }
}
