package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android [NetworkMonitor] backed by [ConnectivityManager] and [NetworkRequest].
 *
 * Uses push-based [ConnectivityManager.NetworkCallback] — no polling needed.
 * Bandwidth values are rounded to the nearest 100 kbps to avoid spurious emissions.
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Uses only ConnectivityManager callbacks (default, fastest).
 * - [ValidationStrategy.HttpOnly]: Validates with HTTP HEAD before reporting online.
 * - [ValidationStrategy.NativeThenHttp]: Reports online immediately, then validates with HTTP HEAD.
 */
internal class AndroidNetworkMonitor(private val context: Context, private val config: NetworkMonitorConfig) :
    NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Suppresses bandwidth updates under memory pressure (T102). */
    private var suppressBandwidthUpdates = false

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private var closed = false
    private val callbackReady = AtomicBoolean(false)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!callbackReady.get()) return
            val caps = connectivityManager.getNetworkCapabilities(network)
            val info = caps.toNetworkInfo()
            handleNativeOnline(info)
        }

        override fun onLost(network: Network) {
            if (!callbackReady.get()) return
            // Check if there's still an active network
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork == null) {
                updateOffline()
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            if (!callbackReady.get()) return
            val newInfo = networkCapabilities.toNetworkInfo()
            // Under memory pressure, skip bandwidth-only updates (T102-T103)
            if (suppressBandwidthUpdates) {
                val current = _networkStatus.value
                if (current is NetworkStatus.Available &&
                    current.info.type == newInfo.type &&
                    current.info.isMetered == newInfo.isMetered
                ) {
                    // Only bandwidth changed — suppress to reduce allocations
                    return
                }
            }
            handleNativeOnline(newInfo)
        }
    }

    /** Memory pressure callback — suppresses bandwidth updates under pressure (T102-T103). */
    private val memoryCallback = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            suppressBandwidthUpdates = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        }

        override fun onConfigurationChanged(newConfig: Configuration) {}

        @Suppress("DEPRECATION")
        override fun onLowMemory() {
            suppressBandwidthUpdates = true
        }
    }

    init {
        // Register callback first, then seed — callback ignores events until seed completes
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        // Android 14+ (API 34): registerNetworkCallback with Handler is deprecated
        // Use the standard callback API which works across all versions
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Register memory pressure callback (T102)
        context.applicationContext.registerComponentCallbacks(memoryCallback)

        // Seed initial state, then allow callback events
        seedInitialState()
        callbackReady.set(true)
    }

    private fun seedInitialState() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        if (activeNetwork != null && caps != null) {
            val info = caps.toNetworkInfo()
            handleNativeOnline(info)
        } else {
            _networkStatus.value = NetworkStatus.Unavailable
            _isOnline.value = false
        }
    }

    private fun handleNativeOnline(info: NetworkInfo) {
        when (config.validationStrategy) {
            ValidationStrategy.NativeOnly -> updateOnline(info)

            ValidationStrategy.HttpOnly -> {
                scope.launch {
                    if (httpHeadCheck()) updateOnline(info) else updateOffline()
                }
            }

            ValidationStrategy.NativeThenHttp -> {
                updateOnline(info) // optimistic native update
                scope.launch {
                    if (!httpHeadCheck()) updateOffline()
                }
            }
        }
    }

    private fun httpHeadCheck(): Boolean = try {
        val conn = URI(config.validationUrl).toURL().openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.connectTimeout = config.validationTimeoutMs.toInt()
        conn.readTimeout = config.validationTimeoutMs.toInt()
        conn.useCaches = false
        val code = conn.responseCode
        conn.disconnect()
        code in 200..399
    } catch (_: Exception) {
        false
    }

    private fun updateOnline(info: NetworkInfo) {
        val newStatus = NetworkStatus.Available(info)
        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return

        _networkStatus.value = newStatus
        _isOnline.value = true

        when {
            oldStatus is NetworkStatus.Unavailable -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Connected(info))
            }

            oldStatus is NetworkStatus.Available -> {
                val oldInfo = oldStatus.info
                if (info.type != oldInfo.type) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.TypeChanged(oldInfo.type, info.type),
                    )
                }
                if (info.isMetered != oldInfo.isMetered) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.MeteredChanged(info.isMetered),
                    )
                }
            }
        }
    }

    private fun updateOffline() {
        val wasOnline = _isOnline.value
        _networkStatus.value = NetworkStatus.Unavailable
        _isOnline.value = false
        if (wasOnline) {
            _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
        }
    }

    override fun close() {
        if (!closed) {
            closed = true
            scope.cancel()
            connectivityManager.unregisterNetworkCallback(networkCallback)
            context.applicationContext.unregisterComponentCallbacks(memoryCallback)
        }
    }
}

/**
 * Extract [NetworkInfo] from [NetworkCapabilities].
 * Returns sensible defaults if capabilities are null.
 */
private fun NetworkCapabilities?.toNetworkInfo(): NetworkInfo {
    if (this == null) return NetworkInfo()

    val type = when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WiFi

        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
            // Detect 5G via high downstream bandwidth heuristic (>100 Mbps typical for 5G NR)
            if (linkDownstreamBandwidthKbps >= 100_000) NetworkType.FiveG else NetworkType.Cellular
        }

        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet

        hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN

        hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.Bluetooth

        else -> NetworkType.Unknown
    }

    val isMetered = !hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)

    // Round bandwidth to nearest 100 kbps to reduce spurious emissions
    val downstream = roundBandwidth(linkDownstreamBandwidthKbps)
    val upstream = roundBandwidth(linkUpstreamBandwidthKbps)

    return NetworkInfo(
        type = type,
        isMetered = isMetered,
        downstreamBandwidthKbps = downstream,
        upstreamBandwidthKbps = upstream,
    )
}

/** Round to nearest 100 kbps. Values <= 0 become -1 (unknown). */
private fun roundBandwidth(kbps: Int): Int {
    if (kbps <= 0) return -1
    return ((kbps + 50) / 100) * 100
}
