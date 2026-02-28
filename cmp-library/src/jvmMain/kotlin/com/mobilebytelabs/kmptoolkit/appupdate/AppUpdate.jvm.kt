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
 * JVM implementation of AppUpdate using custom version check endpoint.
 *
 * This implementation requires a custom version check URL that returns
 * version information in JSON format.
 *
 * ## Expected JSON Format
 *
 * ```json
 * {
 *   "version": "1.2.3",
 *   "updateType": "FLEXIBLE",
 *   "releaseNotes": "Bug fixes and improvements",
 *   "downloadUrl": "https://example.com/download"
 * }
 * ```
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    private var currentVersionOverride: AppVersion? = null

    /**
     * Checks if an update is available from a custom endpoint.
     *
     * Requires [AppUpdateConfig.customVersionCheckUrl] to be set.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        val versionCheckUrl = config.customVersionCheckUrl
            ?: return UpdateResult.NotSupported(
                "JVM requires customVersionCheckUrl in AppUpdateConfig",
            )

        val currentVersion = getCurrentVersion()

        return try {
            val response = fetchUrl(versionCheckUrl)
            parseVersionResponse(response, currentVersion)
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
        val versionCheckUrl = config.customVersionCheckUrl
            ?: return UpdateResult.NotSupported(
                "JVM requires customVersionCheckUrl in AppUpdateConfig",
            )

        return try {
            val response = fetchUrl(versionCheckUrl)
            val downloadUrl = parseDownloadUrl(response)

            if (downloadUrl != null) {
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
        val url = config.customVersionCheckUrl ?: return false
        return openUrl(url)
    }

    /**
     * In-app updates are conditionally supported on JVM.
     * Requires customVersionCheckUrl to be configured.
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
     * Expected format: {"version": "1.2.3", "updateType": "FLEXIBLE", "releaseNotes": "...", "downloadUrl": "..."}
     */
    private fun parseVersionResponse(response: String, currentVersion: AppVersion): UpdateResult {
        return try {
            // Simple JSON parsing without external dependencies
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
            val downloadUrl = downloadUrlMatch?.groupValues?.get(1)

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
            // Try platform-specific commands
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
