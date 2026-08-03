package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

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

    /**
     * Gate for callback events fired before [seedInitialState] completes.
     * Callbacks queue on this deferred via [scope].launch so the first event
     * after construction is not silently dropped (M-002 fix).
     */
    private val seedComplete = CompletableDeferred<Unit>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            scope.launch {
                seedComplete.await()
                val caps = connectivityManager.getNetworkCapabilities(network)
                val info = caps.toNetworkInfo()
                handleNativeOnline(info)
            }
        }

        override fun onLost(network: Network) {
            scope.launch {
                seedComplete.await()
                // Guard against race: after onLost fires, activeNetwork may briefly
                // still point to the just-lost network before the system updates.
                // Verify the remaining active network actually has validated internet.
                if (!hasValidatedInternetAccess()) {
                    updateOffline()
                }
            }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            scope.launch {
                seedComplete.await()
                val newInfo = networkCapabilities.toNetworkInfo()
                // Under memory pressure, skip bandwidth-only updates (T102-T103)
                if (suppressBandwidthUpdates) {
                    val current = _networkStatus.value
                    if (current is NetworkStatus.Available &&
                        current.info.type == newInfo.type &&
                        current.info.isMetered == newInfo.isMetered
                    ) {
                        // Only bandwidth changed — suppress to reduce allocations
                        return@launch
                    }
                }
                handleNativeOnline(newInfo)
            }
        }
    }

    /** Memory pressure callback — suppresses bandwidth updates under pressure (T102-T103). */
    @Suppress("DEPRECATION")
    private val memoryCallback = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            suppressBandwidthUpdates = level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
        }

        override fun onConfigurationChanged(newConfig: Configuration) {}

        override fun onLowMemory() {
            suppressBandwidthUpdates = true
        }
    }

    private val _monitoring = MutableStateFlow(false)
    override val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    // API 23+: require Android's own validation probe (VALIDATED) so callbacks fire only for
    // networks the OS confirmed can reach the internet. Pre-API-23: INTERNET-only (old behaviour).
    private val request: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        .build()

    private var revalidationJob: Job? = null

    @Volatile
    private var is5g = false
    private var telephonyCallback: TelephonyCallback? = null

    init {
        if (config.autoStart) start()
    }

    override fun start() {
        if (closed || _monitoring.value) return
        // Register callback first, then seed — early callback events queue on `seedComplete` (M-002)
        // rather than being dropped. Android 14+ deprecates the Handler overload; standard API works.
        connectivityManager.registerNetworkCallback(request, networkCallback)
        context.applicationContext.registerComponentCallbacks(memoryCallback)
        // try/finally guarantees the gate releases even if seedInitialState() throws. On a re-start
        // seedComplete is already complete, so the guard skips re-completion (callbacks drain live).
        try {
            seedInitialState()
        } finally {
            if (!seedComplete.isCompleted) seedComplete.complete(Unit)
        }
        startRevalidationLoop()
        if (config.detectFiveG) registerTelephony()
        _monitoring.value = true
    }

    override fun stop() {
        if (!_monitoring.value) return
        _monitoring.value = false
        revalidationJob?.cancel()
        revalidationJob = null
        unregisterTelephony()
        runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
        runCatching { context.applicationContext.unregisterComponentCallbacks(memoryCallback) }
    }

    /** Opt-in: re-probe on an interval while online so a silently-dead connection is caught. */
    private fun startRevalidationLoop() {
        if (config.revalidateWhileOnlineMs <= 0L || config.validationStrategy == ValidationStrategy.NativeOnly) return
        revalidationJob?.cancel()
        revalidationJob = scope.launch {
            while (isActive) {
                delay(config.revalidateWhileOnlineMs)
                if (_isOnline.value && !httpHeadCheck()) updateOffline()
            }
        }
    }

    /**
     * Opt-in 5G (NR) detection via [TelephonyDisplayInfo] (API 31+). Requires the READ_PHONE_STATE
     * runtime permission; if it isn't granted the registration throws [SecurityException] and we
     * stay at [NetworkType.Cellular] (best-effort, never crashes). Replaces the old, inaccurate
     * bandwidth heuristic — [with5g] upgrades Cellular → FiveG only on a genuine NR signal.
     */
    private fun registerTelephony() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
            val cb = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
                    is5g = isNr(telephonyDisplayInfo)
                }
            }
            telephonyCallback = cb
            tm.registerTelephonyCallback(context.mainExecutor, cb)
        } catch (_: SecurityException) {
            // READ_PHONE_STATE not granted — stay at Cellular.
        } catch (_: Exception) {
            // Any telephony error — degrade gracefully.
        }
    }

    private fun unregisterTelephony() {
        val cb = telephonyCallback ?: return
        telephonyCallback = null
        is5g = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                    ?.unregisterTelephonyCallback(cb)
            }
        }
    }

    private fun isNr(info: TelephonyDisplayInfo): Boolean {
        val override = info.overrideNetworkType
        return override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA ||
            override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE ||
            override == TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED ||
            info.networkType == TelephonyManager.NETWORK_TYPE_NR
    }

    /** Upgrade Cellular → FiveG when opt-in NR detection is active and NR is currently present. */
    private fun NetworkInfo.with5g(): NetworkInfo =
        if (config.detectFiveG && type == NetworkType.Cellular && is5g) copy(type = NetworkType.FiveG) else this

    private fun seedInitialState() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        // A network flagged as a captive portal has INTERNET but no real connectivity —
        // seed offline so a portal doesn't briefly read online at cold start.
        val isCaptivePortal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        } else {
            false
        }

        // SEED FIX (was: require NET_CAPABILITY_VALIDATED — #113). Android asserts VALIDATED
        // ASYNCHRONOUSLY, so on a cold start where the device is already connected VALIDATED is
        // often not yet set — the old gate seeded offline and (the callback is also VALIDATED-
        // gated) frequently never self-corrected, leaving offline-first stores stuck "No network".
        //
        // Per NetworkMonitorContract invariant #2, seed OPTIMISTICALLY online for an active
        // INTERNET network that isn't a known captive portal — but route through handleNativeOnline
        // so the STRATEGY'S CONFIRMATION runs. With the default NativeThenHttp it reports online
        // immediately AND fires an async HTTP-204 probe that CORRECTS to offline when there is no
        // REAL internet: e.g. cellular data enabled but NO ACTIVE PLAN, LAN-only, or a network that
        // never validates. A raw updateOnline() here left that case stuck "connected" — the
        // VALIDATED-gated callback never fires for a network that never validates. (NativeOnly
        // consumers opt into speed-over-accuracy and may still read such a network as online.)
        if (activeNetwork != null && caps != null && hasInternet && !isCaptivePortal) {
            handleNativeOnline(caps.toNetworkInfo())
        } else {
            updateOffline()
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

    /**
     * Reachability probe with any-success over [NetworkMonitorConfig.effectiveValidationUrls] and a
     * strict 204 sentinel. Online iff the primary OR any fallback endpoint returns the generate_204
     * sentinel — a single blocked/rate-limited/down host no longer reads as false-offline, and a
     * captive portal (200 + login page, or a 3xx redirect) is correctly NOT validated.
     */
    private fun httpHeadCheck(): Boolean = config.effectiveValidationUrls.any { probeUrl(it) }

    private fun probeUrl(url: String): Boolean {
        val t0 = System.currentTimeMillis()
        var code = -1
        val ok = try {
            val conn = URI(url).toURL().openConnection() as HttpURLConnection
            conn.requestMethod = config.validationMethod
            conn.connectTimeout = config.validationTimeoutMs.toInt()
            conn.readTimeout = config.validationTimeoutMs.toInt()
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            code = conn.responseCode
            val len = conn.contentLengthLong
            conn.disconnect()
            // 204 (generate_204 sentinel) = real internet. A captive portal returns 200 with a login
            // page (content-length > 0) or a 3xx redirect — neither is the sentinel → NOT online.
            code == 204 || (code == 200 && len <= 0L)
        } catch (_: Exception) {
            false
        }
        config.onValidationResult?.invoke(
            ValidationResult(
                url = url,
                success = ok,
                statusCode = code.takeIf { it >= 0 },
                latencyMs =
                System.currentTimeMillis() - t0,
            ),
        )
        return ok
    }

    override suspend fun probe(): NetworkStatus = withContext(Dispatchers.IO) {
        // Ground truth NOW — re-query platform capabilities, don't trust the cached flow.
        val activeNetwork = connectivityManager.activeNetwork
        val caps = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isCaptivePortal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true
        } else {
            false
        }
        if (activeNetwork == null || caps == null || !hasInternet || isCaptivePortal) {
            updateOffline()
        } else {
            val info = caps.toNetworkInfo()
            val online = when (config.validationStrategy) {
                ValidationStrategy.NativeOnly -> true
                ValidationStrategy.HttpOnly, ValidationStrategy.NativeThenHttp -> httpHeadCheck()
            }
            if (online) updateOnline(info) else updateOffline()
        }
        _networkStatus.value
    }

    private fun updateOnline(rawInfo: NetworkInfo) {
        val info = rawInfo.with5g()
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

    /**
     * Returns true only if there is an active network with validated internet access.
     * Checks NET_CAPABILITY_VALIDATED (API 23+) to avoid false positives from networks
     * that are "available" but have no actual internet (e.g. captive portal, LAN-only).
     */
    private fun hasValidatedInternetAccess(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    override fun force() {
        scope.launch { probe() }
    }

    override fun close() {
        if (!closed) {
            closed = true
            stop()
            scope.cancel()
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

        // NetworkType.FiveG is intentionally NOT inferred from a bandwidth heuristic: fast LTE-A
        // exceeds 100 Mbps and slow 5G falls under it, so the heuristic mislabels the connection
        // type. Accurate NR detection needs TelephonyManager/ServiceState (API 29+) — a later pass.
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular

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
