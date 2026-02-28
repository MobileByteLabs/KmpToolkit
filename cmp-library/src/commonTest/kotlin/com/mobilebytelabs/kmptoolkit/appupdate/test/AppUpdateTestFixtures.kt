package com.mobilebytelabs.kmptoolkit.appupdate.test

import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
import com.mobilebytelabs.kmptoolkit.appupdate.AppVersion
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateInfo
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType
import com.mobilebytelabs.kmptoolkit.appupdate.VersionInfo

/**
 * Test fixtures for App Update tests.
 * Provides common test data to avoid duplication across tests.
 */
object AppUpdateTestFixtures {

    // ========================================================================
    // AppVersion Fixtures
    // ========================================================================

    val VERSION_1_0_0 = AppVersion.of(1, 0, 0)
    val VERSION_1_2_3 = AppVersion.of(1, 2, 3)
    val VERSION_2_0_0 = AppVersion.of(2, 0, 0)
    val VERSION_1_2_3_WITH_CODE = AppVersion.of(1, 2, 3, versionCode = 10)

    // ========================================================================
    // UpdateInfo Fixtures
    // ========================================================================

    val UPDATE_AVAILABLE = UpdateInfo(
        isAvailable = true,
        currentVersion = VERSION_1_0_0,
        latestVersion = VERSION_2_0_0,
        updateType = UpdateType.FLEXIBLE,
        releaseNotes = "Bug fixes and improvements",
        storeUrl = "https://play.google.com/store/apps/details?id=com.example",
    )

    val UPDATE_NOT_AVAILABLE = UpdateInfo(
        isAvailable = false,
        currentVersion = VERSION_1_0_0,
        latestVersion = VERSION_1_0_0,
        updateType = UpdateType.NONE,
    )

    val IMMEDIATE_UPDATE = UpdateInfo(
        isAvailable = true,
        currentVersion = VERSION_1_0_0,
        latestVersion = VERSION_2_0_0,
        updateType = UpdateType.IMMEDIATE,
        releaseNotes = "Critical security update",
    )

    // ========================================================================
    // VersionInfo Fixtures
    // ========================================================================

    val VERSION_INFO_BASIC = VersionInfo(
        version = "2.0.0",
        updateType = UpdateType.FLEXIBLE,
    )

    val VERSION_INFO_FULL = VersionInfo(
        version = "2.0.0",
        updateType = UpdateType.IMMEDIATE,
        releaseNotes = "New features and bug fixes",
        downloadUrl = "https://example.com/app-2.0.0.apk",
        storeUrl = "https://play.google.com/store/apps/details?id=com.example",
        metadata = mapOf("minSdk" to "21", "targetSdk" to "34"),
    )

    // ========================================================================
    // AppUpdateConfig Fixtures
    // ========================================================================

    val ANDROID_CONFIG = AppUpdateConfig.builder()
        .android(packageName = "com.example.app")
        .build()

    val IOS_CONFIG = AppUpdateConfig.builder()
        .ios(appStoreId = "123456789")
        .countryCode("us")
        .build()

    val MULTI_PLATFORM_CONFIG = AppUpdateConfig.builder()
        .android(packageName = "com.example.app")
        .ios(appStoreId = "123456789")
        .macos(appStoreId = "987654321")
        .jvm(downloadUrl = "https://example.com/app.jar")
        .build()

    // ========================================================================
    // GitHub API Fixtures
    // ========================================================================

    const val GITHUB_RELEASE_RESPONSE = """
{
    "tag_name": "v2.0.0",
    "name": "Version 2.0.0",
    "body": "## What's New\n- Feature A\n- Bug fix B",
    "prerelease": false,
    "assets": [
        {
            "name": "app-windows.exe",
            "browser_download_url": "https://github.com/org/repo/releases/download/v2.0.0/app-windows.exe"
        },
        {
            "name": "app-macos.dmg",
            "browser_download_url": "https://github.com/org/repo/releases/download/v2.0.0/app-macos.dmg"
        },
        {
            "name": "app-linux.AppImage",
            "browser_download_url": "https://github.com/org/repo/releases/download/v2.0.0/app-linux.AppImage"
        }
    ]
}
"""

    const val GITHUB_PRERELEASE_RESPONSE = """
{
    "tag_name": "v2.1.0-beta",
    "name": "Version 2.1.0 Beta",
    "body": "Beta release",
    "prerelease": true,
    "assets": []
}
"""

    // ========================================================================
    // Supabase API Fixtures
    // ========================================================================

    const val SUPABASE_VERSION_RESPONSE = """
[
    {
        "id": "123e4567-e89b-12d3-a456-426614174000",
        "platform": "android",
        "version": "2.0.0",
        "update_type": "FLEXIBLE",
        "release_notes": "Bug fixes",
        "download_url": null,
        "store_url": "https://play.google.com/store/apps/details?id=com.example",
        "is_active": true,
        "created_at": "2026-02-28T10:00:00Z"
    }
]
"""

    const val SUPABASE_EMPTY_RESPONSE = "[]"

    const val SUPABASE_IMMEDIATE_RESPONSE = """
[
    {
        "platform": "android",
        "version": "3.0.0",
        "update_type": "IMMEDIATE",
        "release_notes": "Critical update required"
    }
]
"""
}
