package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import platform.posix.AF_INET
import platform.posix.EINPROGRESS
import platform.posix.F_GETFL
import platform.posix.F_SETFL
import platform.posix.IPPROTO_TCP
import platform.posix.O_NONBLOCK
import platform.posix.POLLOUT
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_ERROR
import platform.posix.addrinfo
import platform.posix.close
import platform.posix.connect
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.getsockopt
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.socket

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
     * Reachability with any-success over [NetworkMonitorConfig.effectiveValidationUrls] via a
     * non-blocking TCP connect — a single blocked/down host no longer reads as false-offline.
     *
     * NOTE (follow-up): TCP-connect can't fully distinguish a captive portal (which also accepts
     * the socket) from real internet. Full parity with the other platforms' HTTP-204 sentinel needs
     * an NSURLSession GET-204 confined to per-leaf-target source sets (iosMain/macosMain/tvosMain/
     * watchosMain): `NSHTTPURLResponse.statusCode` is `NSInteger`, whose bit width differs across
     * Apple targets, so it CANNOT be read in the shared `appleMain` metadata compilation.
     */
    private fun reachabilityCheck(): Boolean = config.effectiveValidationUrls.any { url ->
        val ok = tcpConnect(url)
        config.onValidationResult?.invoke(ValidationResult(url = url, success = ok, statusCode = null, latencyMs = -1L))
        ok
    }

    @Suppress("ReturnCount")
    private fun tcpConnect(url: String): Boolean = memScoped {
        val host = url
            .removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore(":")
        val port = if (url.startsWith("https://")) "443" else "80"

        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_STREAM
        hints.ai_protocol = IPPROTO_TCP

        val result = allocPointerTo<addrinfo>()
        if (getaddrinfo(host, port, hints.ptr, result.ptr) != 0) return false

        val addrInfo = result.value ?: return false
        try {
            val sock = socket(addrInfo.pointed.ai_family, addrInfo.pointed.ai_socktype, addrInfo.pointed.ai_protocol)
            if (sock < 0) return false
            try {
                val flags = fcntl(sock, F_GETFL, 0)
                if (fcntl(sock, F_SETFL, flags or O_NONBLOCK) < 0) return false
                val connectResult = connect(sock, addrInfo.pointed.ai_addr, addrInfo.pointed.ai_addrlen)
                if (connectResult == 0) return true
                if (errno != EINPROGRESS) return false
                val pfd = alloc<pollfd>()
                pfd.fd = sock
                pfd.events = POLLOUT.convert()
                val pollResult = poll(pfd.ptr, 1u, config.validationTimeoutMs.toInt())
                if (pollResult <= 0) return false
                val optVal = alloc<IntVar>()
                val optLen = alloc<UIntVar>()
                optLen.value = sizeOf<IntVar>().convert()
                getsockopt(sock, SOL_SOCKET, SO_ERROR, optVal.ptr, optLen.ptr)
                optVal.value == 0
            } finally {
                close(sock)
            }
        } finally {
            freeaddrinfo(addrInfo)
        }
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
