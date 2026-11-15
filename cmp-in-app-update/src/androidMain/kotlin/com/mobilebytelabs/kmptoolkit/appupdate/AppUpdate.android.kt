package com.mobilebytelabs.kmptoolkit.appupdate

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import com.google.android.play.core.appupdate.AppUpdateManager as PlayAppUpdateManager

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

/**
 * Application context, the resumed Activity, and the Play manager factory.
 *
 * The Activity is needed because Play's update flow is an Activity-scoped UI; the context alone can
 * only ask whether an update exists. Tracked through lifecycle callbacks, as cmp-app-review does,
 * so a consumer passes nothing.
 */
internal object AppUpdateContext {
    @SuppressLint("StaticFieldLeak")
    private var appContext: Context? = null
    internal val resumed = MutableStateFlow<WeakReference<Activity>?>(null)

    /** Swapped for Play's FakeAppUpdateManager in tests. */
    var managerFactory: (Context) -> PlayAppUpdateManager = AppUpdateManagerFactory::create

    val context: Context? get() = appContext

    fun attach(ctx: Context) {
        val app = ctx.applicationContext
        appContext = app
        (app as? Application)?.registerActivityLifecycleCallbacks(Callbacks)
    }

    /**
     * The resumed Activity, waiting up to [timeoutMillis] for one. A check started from app start-up
     * (a ViewModel init, say) can run a frame before the first onResume; failing it there would drop
     * the one update check per launch.
     */
    suspend fun awaitResumedActivity(timeoutMillis: Long = 5_000): Activity? =
        resumed.value?.get() ?: withTimeoutOrNull(timeoutMillis) {
            resumed.first { it?.get() != null }?.get()
        }

    private object Callbacks : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            resumed.value = WeakReference(activity)
        }

        override fun onActivityPaused(activity: Activity) {
            // Cleared on pause so an update flow can never target a dead Activity.
            if (resumed.value?.get() === activity) resumed.value = null
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}

/**
 * ContentProvider that auto-initializes the AppUpdate module.
 */
class AppUpdateInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { AppUpdateContext.attach(it) }
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
        // Check if Android is enabled in config
        if (!config.androidEnabled) {
            return UpdateResult.NotSupported("Android updates disabled in configuration")
        }

        val context = AppUpdateContext.context
            ?: return UpdateResult.Error("Context not available. App not properly initialized.")

        return try {
            val appUpdateManager = AppUpdateContext.managerFactory(context)
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
        val context = AppUpdateContext.context ?: return AppVersion.UNKNOWN

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
     * Starts Play's in-app update flow in the resumed Activity.
     *
     * Before, this checked availability and returned it without launching anything, so no Android
     * app ever saw the update dialog. It now launches the flow, resumes an IMMEDIATE update the user
     * backgrounded, and installs a FLEXIBLE update that has finished downloading.
     */
    actual suspend fun startUpdate(
        updateType: com.mobilebytelabs.kmptoolkit.appupdate.UpdateType,
        config: AppUpdateConfig,
    ): UpdateResult {
        // Check if Android is enabled in config
        if (!config.androidEnabled) {
            return UpdateResult.NotSupported("Android updates disabled in configuration")
        }

        val context = AppUpdateContext.context
            ?: return UpdateResult.Error("Context not available. App not properly initialized.")

        return try {
            val appUpdateManager = AppUpdateContext.managerFactory(context)
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            // A FLEXIBLE download finished earlier: install it rather than offer it again. Play
            // restarts the app, so the task is not awaited.
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
                return UpdateResult.Success(availableInfo(context, appUpdateInfo, updateType))
            }

            val availability = appUpdateInfo.updateAvailability()
            val resuming = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
            if (availability != UpdateAvailability.UPDATE_AVAILABLE && !resuming) {
                return UpdateResult.Success(UpdateInfo.noUpdate(getCurrentVersion()))
            }

            // Only an IMMEDIATE flow can be in progress; resume it as one.
            val playUpdateType = when {
                resuming -> AppUpdateType.IMMEDIATE
                updateType == com.mobilebytelabs.kmptoolkit.appupdate.UpdateType.IMMEDIATE -> AppUpdateType.IMMEDIATE
                else -> AppUpdateType.FLEXIBLE
            }

            if (!resuming && !appUpdateInfo.isUpdateTypeAllowed(playUpdateType)) {
                return UpdateResult.Error(
                    "Update type ${updateType.name} is not allowed for this update",
                )
            }

            val activity = AppUpdateContext.awaitResumedActivity()
                ?: return UpdateResult.Error("No resumed Activity to launch the update flow into")

            val resultCode = appUpdateManager.startUpdateFlow(
                appUpdateInfo,
                activity,
                AppUpdateOptions.newBuilder(playUpdateType).build(),
            ).await()

            when (resultCode) {
                Activity.RESULT_OK -> UpdateResult.Success(availableInfo(context, appUpdateInfo, updateType))
                Activity.RESULT_CANCELED -> UpdateResult.Cancelled
                else -> UpdateResult.Error("Update flow failed (result code $resultCode)")
            }
        } catch (e: Exception) {
            UpdateResult.Error("Failed to start update: ${e.message}", e)
        }
    }

    private fun availableInfo(
        context: Context,
        appUpdateInfo: AppUpdateInfo,
        updateType: com.mobilebytelabs.kmptoolkit.appupdate.UpdateType,
    ) = UpdateInfo.available(
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
    )

    /**
     * Opens Google Play Store to the app's page.
     */
    actual fun openStoreForUpdate(config: AppUpdateConfig): Boolean {
        // Check if Android is enabled in config
        if (!config.androidEnabled) {
            return false
        }

        val context = AppUpdateContext.context ?: return false
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
