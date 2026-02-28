package com.mobilebytelabs.kmptoolkit.appupdate

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Android implementation of AppUpdate using Google Play In-App Updates API.
// This implementation uses AppUpdateManagerFactory to check for and download
// updates directly from Google Play Store without leaving the app.
//
// Zero Configuration:
// No initialization required! The library auto-initializes using ContentProvider.
//
// Update Types:
// - FLEXIBLE: Downloads in background, user can continue using app
// - IMMEDIATE: Blocking update, user must update before continuing

/** Application context obtained automatically via ContentProvider. */
@SuppressLint("StaticFieldLeak")
private var appContext: Context? = null

/**
 * Internal function to set the application context.
 */
internal fun setAppUpdateContext(context: Context) {
    appContext = context.applicationContext
}

/**
 * ContentProvider that auto-initializes the AppUpdate module.
 */
class AppUpdateInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { setAppUpdateContext(it) }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        0
}

actual object AppUpdate {
    /**
     * Checks if an update is available from Google Play.
     */
    actual suspend fun checkForUpdate(config: AppUpdateConfig): UpdateResult {
        val context = appContext
            ?: return UpdateResult.Error("Context not available. App not properly initialized.")

        return try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            val currentVersion = getCurrentVersion()
            val updateAvailability = appUpdateInfo.updateAvailability()

            when (updateAvailability) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateType = determineUpdateType(appUpdateInfo)
                    val latestVersionCode = appUpdateInfo.availableVersionCode().toLong()

                    UpdateResult.Success(
                        UpdateInfo.available(
                            currentVersion = currentVersion,
                            latestVersion = AppVersion(
                                major = 0,
                                minor = 0,
                                patch = 0,
                                versionName = "Unknown",
                                versionCode = latestVersionCode,
                            ),
                            updateType = updateType,
                            storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}",
                        ),
                    )
                }

                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
                }

                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    UpdateResult.Success(
                        UpdateInfo(
                            isAvailable = true,
                            currentVersion = currentVersion,
                            updateType = UpdateType.FLEXIBLE,
                        ),
                    )
                }

                else -> {
                    UpdateResult.Success(UpdateInfo.noUpdate(currentVersion))
                }
            }
        } catch (e: Exception) {
            UpdateResult.Error("Failed to check for updates: ${e.message}", e)
        }
    }

    /**
     * Gets the currently installed app version.
     */
    actual fun getCurrentVersion(): AppVersion {
        val context = appContext ?: return AppVersion.UNKNOWN

        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionName = packageInfo.versionName ?: "0.0.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            AppVersion.parse(versionName)?.copy(versionCode = versionCode)
                ?: AppVersion.UNKNOWN.copy(versionName = versionName, versionCode = versionCode)
        } catch (e: Exception) {
            AppVersion.UNKNOWN
        }
    }

    /**
     * Starts the in-app update flow.
     *
     * Note: This requires an Activity context. For full functionality,
     * use the Android-specific extension functions with Activity.
     */
    actual suspend fun startUpdate(
        updateType: com.mobilebytelabs.kmptoolkit.appupdate.UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult {
        val context = appContext
            ?: return UpdateResult.Error("Context not available. App not properly initialized.")

        return try {
            val appUpdateManager = AppUpdateManagerFactory.create(context)
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                return UpdateResult.Success(UpdateInfo.noUpdate(getCurrentVersion()))
            }

            val playUpdateType = when (updateType) {
                com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
                com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.FLEXIBLE -> AppUpdateType.FLEXIBLE
                else -> AppUpdateType.FLEXIBLE
            }

            if (!appUpdateInfo.isUpdateTypeAllowed(playUpdateType)) {
                return UpdateResult.Error(
                    "Update type ${updateType.name} is not allowed for this update",
                )
            }

            // For starting the actual update flow, we need an Activity
            // Return success with info that update is available
            // The consumer should use Android-specific APIs with Activity
            UpdateResult.Success(
                UpdateInfo.available(
                    currentVersion = getCurrentVersion(),
                    latestVersion = AppVersion(
                        major = 0,
                        minor = 0,
                        patch = 0,
                        versionName = "Unknown",
                        versionCode = appUpdateInfo.availableVersionCode().toLong(),
                    ),
                    updateType = updateType,
                    storeUrl = "https://play.google.com/store/apps/details?id=${context.packageName}",
                ),
            )
        } catch (e: Exception) {
            UpdateResult.Error("Failed to start update: ${e.message}", e)
        }
    }

    /**
     * Opens Google Play Store to the app's page.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        val context = appContext ?: return false
        val packageName = config.packageName ?: context.packageName

        return try {
            // Try Play Store app first
            val playStoreIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName"),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (playStoreIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(playStoreIntent)
                true
            } else {
                // Fall back to web browser
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * In-app updates are supported on Android via Google Play.
     */
    actual fun isSupported(): Boolean = true

    /**
     * Determines the update type based on update priority.
     */
    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): com.mobilebytelabs.kmptoolkit.appupdate.UpdateType {
        val priority = appUpdateInfo.updatePriority()
        return when {
            priority >= 4 -> com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.IMMEDIATE
            priority >= 1 -> com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.FLEXIBLE
            else -> com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.FLEXIBLE
        }
    }
}

/**
 * Extension to await a Task result using coroutines.
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        continuation.resume(result)
    }
    addOnFailureListener { exception ->
        continuation.cancel(exception)
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
