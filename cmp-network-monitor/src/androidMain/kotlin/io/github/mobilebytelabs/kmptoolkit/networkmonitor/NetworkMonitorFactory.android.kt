package io.github.mobilebytelabs.kmptoolkit.networkmonitor

/**
 * Android actual: uses [AndroidNetworkMonitor] backed by ConnectivityManager + NetworkCallback.
 *
 * Context is auto-captured by [NetworkMonitorInitProvider] at app startup.
 * If the ContentProvider is removed, call [setApplicationContext] from Application.onCreate().
 */
actual fun createNetworkMonitor(config: NetworkMonitorConfig): NetworkMonitor {
    val context = appContext
        ?: throw IllegalStateException(
            "NetworkMonitor: Application context not available. " +
                "Ensure NetworkMonitorInitProvider is in your AndroidManifest.xml, " +
                "or call setApplicationContext() from Application.onCreate().",
        )
    return AndroidNetworkMonitor(context, config)
}
