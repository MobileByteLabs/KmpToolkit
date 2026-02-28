package com.mobilebytelabs.kmptoolkit.appupdate

import com.mobilebytelabs.kmptoolkit.appupdate.resolvers.GitHubResolver
import com.mobilebytelabs.kmptoolkit.appupdate.resolvers.SupabaseResolver

/**
 * Configuration for app update checking.
 *
 * Provides platform-specific identifiers, custom endpoints, version resolvers,
 * and platform toggles for controlling which platforms should support app updates.
 *
 * ## Platform Behavior (v0.5.0)
 *
 * All platforms are enabled by default. Use `disable{Platform}()` methods to opt-out.
 * Configure platform-specific settings using the clean builder API.
 *
 * | Platform | Default | Required Config |
 * |----------|---------|-----------------|
 * | Android | Enabled | None (auto-detects) |
 * | iOS | Enabled | None (uses bundle ID) |
 * | macOS | Enabled | None (uses bundle ID) |
 * | JVM | Enabled | versionResolver or versionCheckUrl |
 * | Linux | Enabled | versionResolver or versionCheckUrl |
 * | Windows | Enabled | versionResolver or versionCheckUrl |
 * | tvOS | Enabled | Info only (App Store) |
 * | watchOS | Enabled | Info only (via iPhone) |
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // GitHub Releases (easiest for desktop)
 * val config = AppUpdateConfig.builder()
 *     .android("com.example.app")
 *     .ios("123456789")
 *     .github(owner = "user", repo = "my-app")
 *     .build()
 *
 * // Supabase
 * val config = AppUpdateConfig.builder()
 *     .android("com.example.app")
 *     .ios("123456789")
 *     .supabase(
 *         projectUrl = "https://xxx.supabase.co",
 *         anonKey = "eyJ...",
 *     )
 *     .build()
 *
 * // Custom resolver
 * val config = AppUpdateConfig.builder()
 *     .android("com.example.app")
 *     .versionResolver(MyCustomResolver())
 *     .build()
 * ```
 *
 * @property packageName Android package name (e.g., "com.example.app")
 * @property androidEnabled Whether Android updates are enabled (default: true)
 * @property iosAppStoreId iOS App Store ID (numeric, e.g., "123456789")
 * @property iosEnabled Whether iOS updates are enabled (default: true)
 * @property macosAppStoreId macOS App Store ID (different from iOS)
 * @property macosStoreUrl Direct Mac App Store URL (optional)
 * @property macosEnabled Whether macOS updates are enabled (default: true)
 * @property countryCode Country code for App Store lookup (default: "us")
 * @property versionResolver Custom version resolver for desktop platforms
 * @property customVersionCheckUrl Shared fallback URL for version checking
 * @property jvmVersionCheckUrl JVM-specific version check URL
 * @property jvmDownloadUrl JVM download URL for updates
 * @property jvmEnabled Whether JVM updates are enabled (default: true)
 * @property linuxVersionCheckUrl Linux-specific version check URL
 * @property linuxStoreUrl Linux store URL (Flathub, Snap Store, or direct)
 * @property linuxEnabled Whether Linux updates are enabled (default: true)
 * @property windowsVersionCheckUrl Windows-specific version check URL
 * @property windowsStoreUrl Windows store URL (Microsoft Store or direct)
 * @property windowsEnabled Whether Windows updates are enabled (default: true)
 * @property tvosEnabled Whether tvOS info is shown (default: true)
 * @property watchosEnabled Whether watchOS info is shown (default: true)
 * @since 0.5.0
 */
data class AppUpdateConfig(
    // ═══════════════════════════════════════════════════════════════
    // ANDROID
    // ═══════════════════════════════════════════════════════════════
    val packageName: String? = null,
    val androidEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // iOS
    // ═══════════════════════════════════════════════════════════════
    val iosAppStoreId: String? = null,
    val iosEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // macOS (separate from iOS!)
    // ═══════════════════════════════════════════════════════════════
    val macosAppStoreId: String? = null,
    val macosStoreUrl: String? = null,
    val macosEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // COMMON
    // ═══════════════════════════════════════════════════════════════
    val countryCode: String = "us",
    val versionResolver: VersionResolver? = null,
    val customVersionCheckUrl: String? = null,

    // ═══════════════════════════════════════════════════════════════
    // JVM DESKTOP
    // ═══════════════════════════════════════════════════════════════
    val jvmVersionCheckUrl: String? = null,
    val jvmDownloadUrl: String? = null,
    val jvmEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // LINUX
    // ═══════════════════════════════════════════════════════════════
    val linuxVersionCheckUrl: String? = null,
    val linuxStoreUrl: String? = null,
    val linuxEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // WINDOWS
    // ═══════════════════════════════════════════════════════════════
    val windowsVersionCheckUrl: String? = null,
    val windowsStoreUrl: String? = null,
    val windowsEnabled: Boolean = true,

    // ═══════════════════════════════════════════════════════════════
    // tvOS / watchOS (info only - no manual updates)
    // ═══════════════════════════════════════════════════════════════
    val tvosEnabled: Boolean = true,
    val watchosEnabled: Boolean = true,
) {
    /**
     * Gets the effective JVM version check URL.
     * Uses [jvmVersionCheckUrl] if set, falls back to [customVersionCheckUrl].
     */
    fun getEffectiveJvmVersionCheckUrl(): String? = jvmVersionCheckUrl ?: customVersionCheckUrl

    /**
     * Gets the effective Linux version check URL.
     * Uses [linuxVersionCheckUrl] if set, falls back to [customVersionCheckUrl].
     */
    fun getEffectiveLinuxVersionCheckUrl(): String? = linuxVersionCheckUrl ?: customVersionCheckUrl

    /**
     * Gets the effective Windows version check URL.
     * Uses [windowsVersionCheckUrl] if set, falls back to [customVersionCheckUrl].
     */
    fun getEffectiveWindowsVersionCheckUrl(): String? = windowsVersionCheckUrl ?: customVersionCheckUrl

    /**
     * Builder for creating AppUpdateConfig with clean platform-specific API.
     *
     * ## Quick Start
     *
     * ```kotlin
     * // GitHub Releases (recommended for desktop)
     * val config = AppUpdateConfig.builder()
     *     .android("com.example.app")
     *     .ios("123456789")
     *     .github(owner = "user", repo = "my-app")
     *     .build()
     *
     * // Supabase
     * val config = AppUpdateConfig.builder()
     *     .android("com.example.app")
     *     .supabase(projectUrl = "https://xxx.supabase.co", anonKey = "...")
     *     .build()
     *
     * // Custom resolver
     * val config = AppUpdateConfig.builder()
     *     .versionResolver(MyCustomResolver())
     *     .build()
     * ```
     */
    class Builder {
        // Android
        private var packageName: String? = null
        private var androidEnabled: Boolean = true

        // iOS
        private var iosAppStoreId: String? = null
        private var iosEnabled: Boolean = true

        // macOS
        private var macosAppStoreId: String? = null
        private var macosStoreUrl: String? = null
        private var macosEnabled: Boolean = true

        // Common
        private var countryCode: String = "us"
        private var versionResolver: VersionResolver? = null
        private var customVersionCheckUrl: String? = null

        // JVM
        private var jvmVersionCheckUrl: String? = null
        private var jvmDownloadUrl: String? = null
        private var jvmEnabled: Boolean = true

        // Linux
        private var linuxVersionCheckUrl: String? = null
        private var linuxStoreUrl: String? = null
        private var linuxEnabled: Boolean = true

        // Windows
        private var windowsVersionCheckUrl: String? = null
        private var windowsStoreUrl: String? = null
        private var windowsEnabled: Boolean = true

        // tvOS / watchOS
        private var tvosEnabled: Boolean = true
        private var watchosEnabled: Boolean = true

        // ═══════════════════════════════════════════════════════════════
        // VERSION RESOLVER (Extensible)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Set a custom version resolver for desktop platforms.
         *
         * Use this to integrate with any backend by implementing [VersionResolver].
         * The resolver is used for JVM, Linux, and Windows platforms.
         *
         * ```kotlin
         * class MyResolver : VersionResolver {
         *     override suspend fun resolve(): VersionInfo {
         *         // Your implementation
         *     }
         * }
         *
         * val config = AppUpdateConfig.builder()
         *     .versionResolver(MyResolver())
         *     .build()
         * ```
         *
         * @param resolver Custom implementation of [VersionResolver]
         */
        fun versionResolver(resolver: VersionResolver) = apply {
            this.versionResolver = resolver
        }

        /**
         * Use GitHub Releases for version checking on desktop platforms.
         *
         * Automatically fetches the latest release from a GitHub repository.
         * Extracts version from tag, release notes from body, and download URLs
         * from release assets.
         *
         * ```kotlin
         * // Public repo
         * val config = AppUpdateConfig.builder()
         *     .github(owner = "user", repo = "my-app")
         *     .build()
         *
         * // Private repo
         * val config = AppUpdateConfig.builder()
         *     .github(
         *         owner = "company",
         *         repo = "private-app",
         *         token = BuildConfig.GITHUB_TOKEN,
         *     )
         *     .build()
         * ```
         *
         * @param owner GitHub username or organization
         * @param repo Repository name
         * @param token Optional: Personal access token for private repos
         */
        fun github(owner: String, repo: String, token: String? = null) = apply {
            versionResolver = GitHubResolver(owner, repo, token)
        }

        /**
         * Use Supabase for version checking on desktop platforms.
         *
         * Queries a Supabase table for the latest version information.
         * No Supabase SDK required - uses REST API directly.
         *
         * ```kotlin
         * val config = AppUpdateConfig.builder()
         *     .supabase(
         *         projectUrl = "https://xxx.supabase.co",
         *         anonKey = "eyJ...",
         *     )
         *     .build()
         * ```
         *
         * Required table schema:
         * ```sql
         * CREATE TABLE app_versions (
         *     id SERIAL PRIMARY KEY,
         *     version TEXT NOT NULL,
         *     update_type TEXT DEFAULT 'FLEXIBLE',
         *     release_notes TEXT,
         *     download_url TEXT,
         *     platform TEXT DEFAULT 'all',
         *     created_at TIMESTAMPTZ DEFAULT NOW()
         * );
         * ```
         *
         * @param projectUrl Supabase project URL (e.g., "https://xxx.supabase.co")
         * @param anonKey Supabase anon/public key
         * @param tableName Table name (default: "app_versions")
         */
        fun supabase(projectUrl: String, anonKey: String, tableName: String = "app_versions") = apply {
            versionResolver = SupabaseResolver(projectUrl, anonKey, tableName)
        }

        // ═══════════════════════════════════════════════════════════════
        // PLATFORM-SPECIFIC API
        // ═══════════════════════════════════════════════════════════════

        /**
         * Configure Android with package name for Google Play.
         *
         * @param packageName Package name (e.g., "com.example.app"). If null, auto-detects.
         */
        fun android(packageName: String? = null) = apply {
            this.packageName = packageName
            androidEnabled = true
        }

        /**
         * Configure iOS with App Store ID.
         *
         * @param appStoreId Numeric App Store ID (e.g., "123456789"). If null, uses bundle ID.
         */
        fun ios(appStoreId: String? = null) = apply {
            this.iosAppStoreId = appStoreId
            iosEnabled = true
        }

        /**
         * Configure macOS with Mac App Store ID.
         *
         * Note: macOS App Store ID is different from iOS App Store ID.
         *
         * @param appStoreId Mac App Store ID (numeric)
         * @param storeUrl Direct Mac App Store URL (optional)
         */
        fun macos(appStoreId: String? = null, storeUrl: String? = null) = apply {
            this.macosAppStoreId = appStoreId
            this.macosStoreUrl = storeUrl
            macosEnabled = true
        }

        /**
         * Configure JVM desktop with version check and download URL.
         *
         * Note: If [versionResolver] is set (via [github] or [supabase]),
         * these URLs are used as fallback/override for download location.
         *
         * @param versionCheckUrl API endpoint returning version JSON.
         *        Expected format: {"version": "1.2.3", "updateType": "FLEXIBLE"}
         * @param downloadUrl Direct download URL for the app
         */
        fun jvm(versionCheckUrl: String? = null, downloadUrl: String? = null) = apply {
            this.jvmVersionCheckUrl = versionCheckUrl
            this.jvmDownloadUrl = downloadUrl
            jvmEnabled = true
        }

        /**
         * Configure Linux with version check and store/download URL.
         *
         * Note: If [versionResolver] is set (via [github] or [supabase]),
         * these URLs are used as fallback/override for store location.
         *
         * @param versionCheckUrl API endpoint returning version JSON.
         * @param storeUrl Flathub, Snap Store, or direct download URL.
         */
        fun linux(versionCheckUrl: String? = null, storeUrl: String? = null) = apply {
            this.linuxVersionCheckUrl = versionCheckUrl
            this.linuxStoreUrl = storeUrl
            linuxEnabled = true
        }

        /**
         * Configure Windows with version check and Microsoft Store URL.
         *
         * Note: If [versionResolver] is set (via [github] or [supabase]),
         * these URLs are used as fallback/override for store location.
         *
         * @param versionCheckUrl API endpoint returning version JSON.
         * @param storeUrl Microsoft Store URL or direct download.
         */
        fun windows(versionCheckUrl: String? = null, storeUrl: String? = null) = apply {
            this.windowsVersionCheckUrl = versionCheckUrl
            this.windowsStoreUrl = storeUrl
            windowsEnabled = true
        }

        // ═══════════════════════════════════════════════════════════════
        // DISABLE METHODS (Explicit opt-out)
        // ═══════════════════════════════════════════════════════════════

        /**
         * Disables Android platform support.
         * When disabled, Android will return [UpdateResult.NotSupported].
         */
        fun disableAndroid() = apply { androidEnabled = false }

        /**
         * Disables iOS platform support.
         * When disabled, iOS will return [UpdateResult.NotSupported].
         */
        fun disableIos() = apply { iosEnabled = false }

        /**
         * Disables macOS platform support.
         * When disabled, macOS will return [UpdateResult.NotSupported].
         */
        fun disableMacos() = apply { macosEnabled = false }

        /**
         * Disables JVM platform support.
         * When disabled, JVM will return [UpdateResult.NotSupported].
         */
        fun disableJvm() = apply { jvmEnabled = false }

        /**
         * Disables Linux platform support.
         * When disabled, Linux will return [UpdateResult.NotSupported].
         */
        fun disableLinux() = apply { linuxEnabled = false }

        /**
         * Disables Windows platform support.
         * When disabled, Windows will return [UpdateResult.NotSupported].
         */
        fun disableWindows() = apply { windowsEnabled = false }

        /**
         * Disables tvOS platform support.
         * When disabled, tvOS will return [UpdateResult.NotSupported].
         */
        fun disableTvos() = apply { tvosEnabled = false }

        /**
         * Disables watchOS platform support.
         * When disabled, watchOS will return [UpdateResult.NotSupported].
         */
        fun disableWatchos() = apply { watchosEnabled = false }

        // ═══════════════════════════════════════════════════════════════
        // COMMON CONFIG
        // ═══════════════════════════════════════════════════════════════

        /**
         * Sets the country code for App Store lookup.
         *
         * @param code ISO 3166-1 alpha-2 country code (default: "us")
         */
        fun countryCode(code: String) = apply { countryCode = code }

        /**
         * Sets a shared version check URL for all desktop platforms.
         *
         * Platform-specific URLs (jvmVersionCheckUrl, linuxVersionCheckUrl,
         * windowsVersionCheckUrl) override this shared URL.
         *
         * Note: If [versionResolver] is set, this URL is not used for
         * version checking but may be used as a fallback.
         *
         * @param url The custom version check endpoint
         */
        fun customVersionCheckUrl(url: String) = apply { customVersionCheckUrl = url }

        // ═══════════════════════════════════════════════════════════════
        // CONVENIENCE PRESETS
        // ═══════════════════════════════════════════════════════════════

        /**
         * Disables all platforms.
         *
         * Use this as a starting point, then enable specific platforms.
         */
        fun disableAllPlatforms() = apply {
            androidEnabled = false
            iosEnabled = false
            macosEnabled = false
            jvmEnabled = false
            linuxEnabled = false
            windowsEnabled = false
            tvosEnabled = false
            watchosEnabled = false
        }

        /**
         * Configures for mobile platforms only (Android + iOS).
         *
         * Desktop platforms will return [UpdateResult.NotSupported].
         */
        fun mobileOnly() = apply {
            androidEnabled = true
            iosEnabled = true
            macosEnabled = false
            jvmEnabled = false
            linuxEnabled = false
            windowsEnabled = false
            tvosEnabled = false
            watchosEnabled = false
        }

        /**
         * Configures for Apple platforms only (iOS + macOS).
         *
         * Non-Apple platforms will return [UpdateResult.NotSupported].
         */
        fun appleOnly() = apply {
            androidEnabled = false
            iosEnabled = true
            macosEnabled = true
            jvmEnabled = false
            linuxEnabled = false
            windowsEnabled = false
            tvosEnabled = true
            watchosEnabled = true
        }

        /**
         * Configures for desktop platforms only (macOS, JVM, Linux, Windows).
         *
         * Mobile platforms will return [UpdateResult.NotSupported].
         */
        fun desktopOnly() = apply {
            androidEnabled = false
            iosEnabled = false
            macosEnabled = true
            jvmEnabled = true
            linuxEnabled = true
            windowsEnabled = true
            tvosEnabled = false
            watchosEnabled = false
        }

        /**
         * Builds the AppUpdateConfig instance.
         */
        fun build() = AppUpdateConfig(
            packageName = packageName,
            androidEnabled = androidEnabled,
            iosAppStoreId = iosAppStoreId,
            iosEnabled = iosEnabled,
            macosAppStoreId = macosAppStoreId,
            macosStoreUrl = macosStoreUrl,
            macosEnabled = macosEnabled,
            countryCode = countryCode,
            versionResolver = versionResolver,
            customVersionCheckUrl = customVersionCheckUrl,
            jvmVersionCheckUrl = jvmVersionCheckUrl,
            jvmDownloadUrl = jvmDownloadUrl,
            jvmEnabled = jvmEnabled,
            linuxVersionCheckUrl = linuxVersionCheckUrl,
            linuxStoreUrl = linuxStoreUrl,
            linuxEnabled = linuxEnabled,
            windowsVersionCheckUrl = windowsVersionCheckUrl,
            windowsStoreUrl = windowsStoreUrl,
            windowsEnabled = windowsEnabled,
            tvosEnabled = tvosEnabled,
            watchosEnabled = watchosEnabled,
        )
    }

    companion object {
        /**
         * Creates a new Builder instance.
         */
        fun builder() = Builder()

        /**
         * Default configuration with all platforms enabled.
         *
         * - Android: Enabled (auto-detects package)
         * - iOS: Enabled (uses bundle ID)
         * - macOS: Enabled (uses bundle ID)
         * - JVM/Linux/Windows: Enabled (require versionResolver or version check URL)
         * - tvOS/watchOS: Enabled (info only)
         */
        val Default = AppUpdateConfig()
    }
}
