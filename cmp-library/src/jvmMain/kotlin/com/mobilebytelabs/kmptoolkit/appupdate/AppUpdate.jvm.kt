package com.mobilebytelabs.kmptoolkit.appupdate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Properties

/**
 * JVM implementation of AppUpdate.
 *
 * Supports multiple version checking methods (in order of priority):
 * 1. [VersionResolver] - GitHub, Supabase, or custom implementation
 * 2. Custom version check URL - Direct JSON endpoint
 *
 * ## Usage with Version Resolver (Recommended)
 *
 * ```kotlin
 * // GitHub Releases
 * val config = AppUpdateConfig.builder()
 *     .github(owner = "user", repo = "my-app")
 *     .build()
 *
 * // Supabase
 * val config = AppUpdateConfig.builder()
 *     .supabase(projectUrl = "https://xxx.supabase.co", anonKey = "...")
 *     .build()
 * ```
 *
 * ## Usage with Custom URL
 *
 * ```kotlin
 * val config = AppUpdateConfig.builder()
 *     .jvm(
 *         versionCheckUrl = "https://api.example.com/version",
 *         downloadUrl = "https://example.com/download",
 *     )
 *     .build()
 * ```
 *
 * @since 0.5.0
 */
actual object AppUpdate {
    private var currentVersionOverride: AppVersion? = null

    /**
     * Checks if an update is available.
     *
     * Version check priority:
     * 1. [AppUpdateConfig.versionResolver] (GitHub, Supabase, custom)
     * 2. [AppUpdateConfig.jvmVersionCheckUrl] or [AppUpdateConfig.customVersionCheckUrl]
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        // Check if JVM is enabled
        if (!config.jvmEnabled) {
            return UpdateResult.NotSupported("JVM updates disabled in configuration")
        }

        val currentVersion = getCurrentVersion()

        // Priority 1: Use version resolver if configured
        config.versionResolver?.let { resolver ->
            return try {
                val versionInfo = resolver.resolve()
                val latestVersion = versionInfo.toAppVersion()
                    ?: return UpdateResult.Error("Failed to parse version: ${versionInfo.version}")

                // Use configured downloadUrl if available, otherwise use resolver's
                val downloadUrl = config.jvmDownloadUrl ?: versionInfo.downloadUrl

                if (currentVersion.isOlderThan(latestVersion)) {
                    UpdateResult.Success(
                        UpdateInfo.available(
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                            updateType = versionInfo.updateType,
                            releaseNotes = versionInfo.releaseNotes,
                            storeUrl = downloadUrl ?: versionInfo.storeUrl,
                        ),
                    )
                } else {
                    UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
                }
            } catch (e: Exception) {
                UpdateResult.Error("Failed to check for updates: ${e.message}", e)
            }
        }

        // Priority 2: Use custom version check URL
        val versionCheckUrl = config.getEffectiveJvmVersionCheckUrl()
            ?: return UpdateResult.NotSupported(
                "JVM requires versionResolver (github/supabase) or versionCheckUrl in AppUpdateConfig",
            )

        return try {
            val response = fetchUrl(versionCheckUrl)
            parseVersionResponse(response, currentVersion, config)
        } catch (e: Exception) {
            UpdateResult.Error("Failed to check for updates: ${e.message}", e)
        }
    }

    /**
     * Gets the currently installed app version.
     *
     * Attempts to read version from:
     * 1. Manually set override
     * 2. version.properties resource file
     * 3. MANIFEST.MF Implementation-Version
     * 4. System property "app.version"
     */
    actual fun getCurrentVersion(): AppVersion {
        currentVersionOverride?.let { return it }

        // Try version.properties
        try {
            val props = Properties()
            javaClass.getResourceAsStream("/version.properties")?.use { stream ->
                props.load(stream)
                val version = props.getProperty("version")
                if (version != null) {
                    AppVersion.parse(version)?.let { return it }
                }
            }
        } catch (_: Exception) {
            // Ignore and try next method
        }

        // Try MANIFEST.MF
        try {
            val pkg = javaClass.`package`
            pkg?.implementationVersion?.let { version ->
                AppVersion.parse(version)?.let { return it }
            }
        } catch (_: Exception) {
            // Ignore and try next method
        }

        // Try system property
        System.getProperty("app.version")?.let { version ->
            AppVersion.parse(version)?.let { return it }
        }

        return AppVersion.UNKNOWN
    }

    /**
     * Opens the download URL for the update.
     */
    actual suspend fun startUpdate(updateType: UpdateType, config: AppUpdateConfig): UpdateResult {
        // Check if JVM is enabled
        if (!config.jvmEnabled) {
            return UpdateResult.NotSupported("JVM updates disabled in configuration")
        }

        // First try to use configured jvmDownloadUrl directly
        config.jvmDownloadUrl?.let { downloadUrl ->
            val opened = openUrl(downloadUrl)
            return if (opened) {
                UpdateResult.Success(
                    UpdateInfo(
                        isAvailable = true,
                        currentVersion = getCurrentVersion(),
                        updateType = updateType,
                        storeUrl = downloadUrl,
                    ),
                )
            } else {
                UpdateResult.Error("Failed to open download URL")
            }
        }

        // Try version resolver to get download URL
        config.versionResolver?.let { resolver ->
            return try {
                val versionInfo = resolver.resolve()
                val downloadUrl = versionInfo.downloadUrl ?: versionInfo.storeUrl
                    ?: return UpdateResult.Error("No download URL available from resolver")

                val opened = openUrl(downloadUrl)
                if (opened) {
                    UpdateResult.Success(
                        UpdateInfo(
                            isAvailable = true,
                            currentVersion = getCurrentVersion(),
                            updateType = updateType,
                            storeUrl = downloadUrl,
                        ),
                    )
                } else {
                    UpdateResult.Error("Failed to open download URL")
                }
            } catch (e: Exception) {
                UpdateResult.Error("Failed to start update: ${e.message}", e)
            }
        }

        // Fall back to fetching version check URL
        val versionCheckUrl = config.getEffectiveJvmVersionCheckUrl()
            ?: return UpdateResult.NotSupported(
                "JVM requires jvmDownloadUrl, versionResolver, or jvmVersionCheckUrl in AppUpdateConfig",
            )

        return try {
            val response = fetchUrl(versionCheckUrl)
            val parsedDownloadUrl = parseDownloadUrl(response)

            if (parsedDownloadUrl != null) {
                val opened = openUrl(parsedDownloadUrl)
                if (opened) {
                    UpdateResult.Success(
                        UpdateInfo(
                            isAvailable = true,
                            currentVersion = getCurrentVersion(),
                            updateType = updateType,
                            storeUrl = parsedDownloadUrl,
                        ),
                    )
                } else {
                    UpdateResult.Error("Failed to open download URL")
                }
            } else {
                UpdateResult.Error("No download URL found in version response")
            }
        } catch (e: Exception) {
            UpdateResult.Error("Failed to start update: ${e.message}", e)
        }
    }

    /**
     * Opens the download URL in the default browser.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        // Check if JVM is enabled
        if (!config.jvmEnabled) {
            return false
        }

        // Use jvmDownloadUrl if available
        val url = config.jvmDownloadUrl ?: return false
        return openUrl(url)
    }

    /**
     * In-app updates are supported on JVM.
     */
    actual fun isSupported(): Boolean = true

    /**
     * Sets the current version manually.
     * Useful for testing or when version can't be detected automatically.
     */
    fun setCurrentVersion(version: AppVersion) {
        currentVersionOverride = version
    }

    /**
     * Fetches content from URL.
     */
    private suspend fun fetchUrl(urlString: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Parses version response JSON.
     */
    private fun parseVersionResponse(
        response: String,
        currentVersion: AppVersion,
        config: AppUpdateConfig,
    ): UpdateResult {
        return try {
            val versionMatch = Regex(""""version"\s*:\s*"([^"]+)"""").find(response)
            val updateTypeMatch = Regex(""""updateType"\s*:\s*"([^"]+)"""").find(response)
            val releaseNotesMatch = Regex(""""releaseNotes"\s*:\s*"([^"]+)"""").find(response)
            val downloadUrlMatch = Regex(""""downloadUrl"\s*:\s*"([^"]+)"""").find(response)

            val latestVersionString = versionMatch?.groupValues?.get(1)
                ?: return UpdateResult.Error("No version found in response")

            val latestVersion = AppVersion.parse(latestVersionString)
                ?: return UpdateResult.Error("Failed to parse version: $latestVersionString")

            val updateType = when (updateTypeMatch?.groupValues?.get(1)?.uppercase()) {
                "IMMEDIATE" -> UpdateType.IMMEDIATE
                "FLEXIBLE" -> UpdateType.FLEXIBLE
                else -> UpdateType.FLEXIBLE
            }

            val releaseNotes = releaseNotesMatch?.groupValues?.get(1)
            val downloadUrl = config.jvmDownloadUrl ?: downloadUrlMatch?.groupValues?.get(1)

            if (currentVersion.isOlderThan(latestVersion)) {
                UpdateResult.Success(
                    UpdateInfo.available(
                        currentVersion = currentVersion,
                        latestVersion = latestVersion,
                        updateType = updateType,
                        releaseNotes = releaseNotes,
                        storeUrl = downloadUrl,
                    ),
                )
            } else {
                UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
            }
        } catch (e: Exception) {
            UpdateResult.Error("Failed to parse version response: ${e.message}", e)
        }
    }

    /**
     * Extracts download URL from response.
     */
    private fun parseDownloadUrl(response: String): String? {
        val match = Regex(""""downloadUrl"\s*:\s*"([^"]+)"""").find(response)
        return match?.groupValues?.get(1)
    }

    /**
     * Opens URL in default browser.
     */
    private fun openUrl(urlString: String): Boolean = try {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI(urlString))
            true
        } else {
            val os = System.getProperty("os.name").lowercase()
            val command = when {
                os.contains("win") -> arrayOf("rundll32", "url.dll,FileProtocolHandler", urlString)
                os.contains("mac") -> arrayOf("open", urlString)
                else -> arrayOf("xdg-open", urlString)
            }
            Runtime.getRuntime().exec(command)
            true
        }
    } catch (e: Exception) {
        false
    }
}
