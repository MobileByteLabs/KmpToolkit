package com.mobilebytelabs.kmptoolkit.appupdate

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AppKit.NSWorkspace
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setHTTPMethod
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * macOS implementation of AppUpdate using iTunes Lookup API.
 *
 * This implementation uses the iTunes Search API to check for app updates
 * and opens the Mac App Store for manual updates.
 *
 * @since 0.3.0
 */
actual object AppUpdate {
    /**
     * Checks if an update is available from the Mac App Store.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        val currentVersion = getCurrentVersion()
        val bundleId = NSBundle.mainBundle.bundleIdentifier
            ?: return UpdateResult.Error("Bundle identifier not found")

        val countryCode = config.countryCode

        // Use custom URL if provided, otherwise use iTunes Lookup API
        val lookupUrl = config.customVersionCheckUrl
            ?: "https://itunes.apple.com/lookup?bundleId=$bundleId&country=$countryCode"

        return try {
            val jsonData = fetchUrl(lookupUrl)
            parseAppStoreResponse(jsonData, currentVersion)
        } catch (e: Exception) {
            UpdateResult.Error("Failed to check for updates: ${e.message}", e)
        }
    }

    /**
     * Gets the currently installed app version from Info.plist.
     */
    actual fun getCurrentVersion(): AppVersion {
        val bundle = NSBundle.mainBundle
        val versionString = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
        val buildString = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String

        val versionName = versionString ?: "0.0.0"
        val versionCode = buildString?.toLongOrNull()

        return AppVersion.parse(versionName)?.copy(versionCode = versionCode)
            ?: AppVersion.UNKNOWN.copy(versionName = versionName, versionCode = versionCode)
    }

    /**
     * Opens the Mac App Store to the app's page for update.
     */
    actual suspend fun startUpdate(
        updateType: UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult {
        val opened = openStoreForUpdate(config)
        return if (opened) {
            UpdateResult.Success(
                UpdateInfo(
                    isAvailable = true,
                    currentVersion = getCurrentVersion(),
                    updateType = updateType,
                ),
            )
        } else {
            UpdateResult.Error("Failed to open Mac App Store")
        }
    }

    /**
     * Opens the Mac App Store to the app's page.
     */
    @OptIn(ExperimentalForeignApi::class)
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        val appStoreId = config.appStoreId
        val bundleId = NSBundle.mainBundle.bundleIdentifier

        val storeUrl = when {
            appStoreId != null -> "macappstore://itunes.apple.com/app/id$appStoreId"
            bundleId != null -> "macappstore://itunes.apple.com/app/$bundleId"
            else -> return false
        }

        val url = NSURL.URLWithString(storeUrl) ?: return false
        return NSWorkspace.sharedWorkspace.openURL(url)
    }

    /**
     * In-app updates are partially supported on macOS.
     */
    actual fun isSupported(): Boolean = true

    /**
     * Fetches data from URL using NSURLSession.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun fetchUrl(urlString: String): NSData {
        val url = NSURL.URLWithString(urlString)
            ?: throw IllegalArgumentException("Invalid URL: $urlString")

        val request = NSMutableURLRequest.requestWithURL(url).apply {
            setHTTPMethod("GET")
        }

        return suspendCancellableCoroutine { continuation ->
            val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, _, error ->
                when {
                    error != null -> {
                        continuation.resumeWithException(
                            Exception("Network error: ${error.localizedDescription}"),
                        )
                    }
                    data != null -> {
                        continuation.resume(data)
                    }
                    else -> {
                        continuation.resumeWithException(Exception("No data received"))
                    }
                }
            }
            task.resume()

            continuation.invokeOnCancellation {
                task.cancel()
            }
        }
    }

    /**
     * Parses the iTunes Lookup API response.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun parseAppStoreResponse(data: NSData, currentVersion: AppVersion): UpdateResult {
        return memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val json = NSJSONSerialization.JSONObjectWithData(
                data,
                0u,
                errorPtr.ptr,
            ) as? Map<*, *>

            if (errorPtr.value != null || json == null) {
                return UpdateResult.Error("Failed to parse App Store response")
            }

            val results = json["results"] as? List<*>
            if (results.isNullOrEmpty()) {
                return UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
            }

            val appInfo = results.first() as? Map<*, *>
                ?: return UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))

            val latestVersionString = appInfo["version"] as? String
                ?: return UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))

            val latestVersion = AppVersion.parse(latestVersionString)
                ?: return UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))

            val releaseNotes = appInfo["releaseNotes"] as? String
            val trackViewUrl = appInfo["trackViewUrl"] as? String

            if (currentVersion.isOlderThan(latestVersion)) {
                UpdateResult.Success(
                    UpdateInfo.available(
                        currentVersion = currentVersion,
                        latestVersion = latestVersion,
                        updateType = UpdateType.FLEXIBLE,
                        releaseNotes = releaseNotes,
                        storeUrl = trackViewUrl,
                    ),
                )
            } else {
                UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
            }
        }
    }
}
