package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_loopback
import platform.Network.nw_interface_type_other
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_t
import platform.Network.nw_path_status_satisfiable
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.dispatch_queue_create
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.dataTaskWithRequest

/**
 * Apple [NetworkMonitor] backed by NWPathMonitor.
 *
 * Works on iOS, macOS, tvOS, and watchOS via the shared `appleMain` source set.
 * Uses push-based path updates — no polling needed.
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Uses only NWPathMonitor (default, fastest).
 * - [ValidationStrategy.HttpOnly]: Validates with TCP connect before reporting online.
 * - [ValidationStrategy.NativeThenHttp]: Reports online immediately, then validates with TCP connect.
 */
@OptIn(ExperimentalForeignApi::class)
internal class AppleNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private var closed = false
    private val monitor: nw_path_monitor_t = nw_path_monitor_create()
    private val queue = dispatch_queue_create(
        "io.github.mobilebytelabs.kmptoolkit.networkmonitor",
        null,
    )

    private val _monitoring = MutableStateFlow(false)
    override val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    private var revalidationJob: Job? = null
    private var nativeCancelled = false

    private fun cancelNative() {
        if (!nativeCancelled) {
            nativeCancelled = true
            nw_path_monitor_cancel(monitor)
        }
    }

    init {
        nw_path_monitor_set_queue(monitor, queue)
        nw_path_monitor_set_update_handler(monitor) { path ->
            val status = nw_path_get_status(path)
            // nw_path_status_satisfied = Apple confirmed the path can send/receive data
            // (analogous to NET_CAPABILITY_VALIDATED on Android).
            // nw_path_status_satisfiable = path is NOT active but could be on-demand
            // (e.g. cellular in low-data mode, on-demand VPN) — treat as offline.
            val isSatisfied = status == nw_path_status_satisfied

            if (isSatisfied) {
                val type = when {
                    nw_path_uses_interface_type(path, nw_interface_type_wifi) -> NetworkType.WiFi
                    nw_path_uses_interface_type(path, nw_interface_type_cellular) -> NetworkType.Cellular
                    nw_path_uses_interface_type(path, nw_interface_type_wired) -> NetworkType.Ethernet
                    nw_path_uses_interface_type(path, nw_interface_type_loopback) -> NetworkType.Unknown
                    nw_path_uses_interface_type(path, nw_interface_type_other) -> NetworkType.Unknown
                    else -> NetworkType.Unknown
                }
                val isMetered = nw_path_is_expensive(path) || nw_path_is_constrained(path)
                val info = NetworkInfo(type = type, isMetered = isMetered)
                handleNativeOnline(info)
            } else {
                updateOffline()
            }
        }
        if (config.autoStart) start()
    }

    override fun start() {
        if (closed || _monitoring.value) return
        nw_path_monitor_start(monitor)
        _monitoring.value = true
        startRevalidationLoop()
    }

    /**
     * NOTE: NWPathMonitor's `cancel` is terminal — it cannot be resumed. On Apple, [stop] therefore
     * releases the native monitor like [close] does for the native part; [start] after [stop] is a
     * guarded no-op. Use [close] for full teardown.
     */
    override fun stop() {
        if (!_monitoring.value) return
        _monitoring.value = false
        revalidationJob?.cancel()
        revalidationJob = null
        cancelNative()
    }

    /** Opt-in: re-probe on an interval while online so a silently-dead connection is caught. */
    private fun startRevalidationLoop() {
        if (config.revalidateWhileOnlineMs <= 0L || config.validationStrategy == ValidationStrategy.NativeOnly) return
        revalidationJob?.cancel()
        revalidationJob = scope.launch {
            while (isActive) {
                delay(config.revalidateWhileOnlineMs)
                if (_isOnline.value && !reachabilityCheck()) updateOffline()
            }
        }
    }

    private fun handleNativeOnline(info: NetworkInfo) {
        when (config.validationStrategy) {
            ValidationStrategy.NativeOnly -> updateOnline(info)

            ValidationStrategy.HttpOnly -> {
                scope.launch {
                    if (reachabilityCheck()) updateOnline(info) else updateOffline()
                }
            }

            ValidationStrategy.NativeThenHttp -> {
                updateOnline(info) // optimistic native update
                scope.launch {
                    if (!reachabilityCheck()) updateOffline()
                }
            }
        }
    }

    /**
     * Reachability with any-success over [NetworkMonitorConfig.effectiveValidationUrls] using an
     * HTTP **204 sentinel** — real internet only if an endpoint returns HTTP 204 (generate_204).
     * A captive portal redirects generate_204 to its login page (NSURLSession follows the redirect
     * → final status 200, not 204), so it is correctly NOT validated — the portal accuracy the raw
     * TCP connect could not provide. This brings Apple to full parity with Android/Jvm/Js.
     */
    private suspend fun reachabilityCheck(): Boolean {
        for (url in config.effectiveValidationUrls) {
            if (httpProbe(url)) return true
        }
        return false
    }

    /** One HTTP validation probe via NSURLSession; online iff the endpoint returns HTTP 204. */
    private suspend fun httpProbe(url: String): Boolean = suspendCancellableCoroutine { cont ->
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl == null) {
            config.onValidationResult?.invoke(ValidationResult(url, false, null, -1L))
            if (cont.isActive) cont.resume(false)
            return@suspendCancellableCoroutine
        }
        // Ephemeral, no cookies: a portal's cookie must not fake "logged in"; fail fast.
        val sessionConfig = NSURLSessionConfiguration.ephemeralSessionConfiguration().apply {
            timeoutIntervalForRequest = config.validationTimeoutMs / 1000.0
            waitsForConnectivity = false
            requestCachePolicy = NSURLRequestReloadIgnoringLocalCacheData
            HTTPShouldSetCookies = false
        }
        val session = NSURLSession.sessionWithConfiguration(sessionConfig)
        // Default GET — generate_204 returns 204 to GET. (config.validationMethod is honored on the
        // HttpURLConnection/XHR platforms; NSURLSession uses GET here to avoid an interop setter.)
        val request = NSMutableURLRequest(uRL = nsUrl)
        val task = session.dataTaskWithRequest(request) { _, response, error ->
            val code = (response as? NSHTTPURLResponse)?.statusCode?.toLong()
            val ok = error == null && code == 204L
            config.onValidationResult?.invoke(ValidationResult(url, ok, code?.toInt(), -1L))
            if (cont.isActive) cont.resume(ok)
        }
        cont.invokeOnCancellation { task.cancel() }
        task.resume()
    }

    override suspend fun probe(): NetworkStatus = withContext(Dispatchers.IO) {
        // NWPathMonitor pushes current state; when we believe we're online, actively re-confirm
        // reachability now so a silently-dead connection is corrected on demand.
        if (_networkStatus.value is NetworkStatus.Available) {
            val online = when (config.validationStrategy) {
                ValidationStrategy.NativeOnly -> true
                ValidationStrategy.HttpOnly, ValidationStrategy.NativeThenHttp -> reachabilityCheck()
            }
            if (!online) updateOffline()
        }
        _networkStatus.value
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

    override fun force() {
        scope.launch { probe() }
    }

    override fun close() {
        if (!closed) {
            closed = true
            _monitoring.value = false
            revalidationJob?.cancel()
            scope.cancel()
            cancelNative()
        }
    }
}
