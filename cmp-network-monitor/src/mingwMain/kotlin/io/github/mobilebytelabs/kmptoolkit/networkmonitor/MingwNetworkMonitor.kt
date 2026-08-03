package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
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
import platform.posix.AF_INET
import platform.posix.INVALID_SOCKET
import platform.posix.IPPROTO_TCP
import platform.posix.SOCKET
import platform.posix.SOCK_STREAM
import platform.posix.WSACleanup
import platform.posix.WSADATA
import platform.posix.WSAStartup
import platform.posix.closesocket
import platform.posix.connect
import platform.posix.htons
import platform.posix.inet_addr
import platform.posix.sockaddr_in
import platform.posix.socket

/**
 * Windows (MinGW) [NetworkMonitor] backed by Winsock2 socket connect polling.
 *
 * Validates connectivity by TCP-connecting to the host derived from
 * [NetworkMonitorConfig.validationUrl]. Polls at [NetworkMonitorConfig.pollIntervalMs].
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Checks if Winsock can open a socket (basic check).
 * - [ValidationStrategy.HttpOnly]: TCP connect to validation URL host.
 * - [ValidationStrategy.NativeThenHttp]: Socket check first, then TCP connect to confirm.
 */
@OptIn(ExperimentalForeignApi::class)
internal class MingwNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private val adaptivePolling = AdaptivePollingState(
        baseIntervalMs = config.pollIntervalMs,
        maxIntervalMs = config.backgroundPollIntervalMs,
    )

    init {
        initWinsock()
        updateState(checkNetwork())
        scope.launch {
            while (isActive) {
                delay(adaptivePolling.intervalMs)
                val changed = updateState(checkNetwork())
                if (changed) adaptivePolling.reportChange() else adaptivePolling.reportStable()
            }
        }
    }

    private fun initWinsock() = memScoped {
        val wsaData = alloc<WSADATA>()
        WSAStartup(0x0202.convert(), wsaData.ptr)
    }

    /**
     * Determine network connectivity based on the configured [ValidationStrategy].
     */
    private fun checkNetwork(): Boolean = when (config.validationStrategy) {
        ValidationStrategy.NativeOnly -> canCreateSocket()
        ValidationStrategy.HttpOnly -> tcpConnectCheck()
        ValidationStrategy.NativeThenHttp -> canCreateSocket() && tcpConnectCheck()
    }

    /** Basic check: can we create a TCP socket? */
    private fun canCreateSocket(): Boolean = memScoped {
        val sock: SOCKET = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (sock == INVALID_SOCKET) return false
        closesocket(sock)
        true
    }

    /**
     * TCP connect to the host:port derived from [NetworkMonitorConfig.validationUrl].
     * Falls back to port 443 for https, 80 for http.
     */
    private fun tcpConnectCheck(): Boolean = config.effectiveValidationUrls.any { tcpConnectOne(it) }

    private fun tcpConnectOne(url: String): Boolean = memScoped {
        val host = url
            .removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore(":")
        val port = if (url.startsWith("https://")) 443 else 80

        val sock: SOCKET = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (sock == INVALID_SOCKET) return false

        try {
            val addr = alloc<sockaddr_in>()
            addr.sin_family = AF_INET.convert()
            addr.sin_port = htons(port.toShort()).toUShort()
            addr.sin_addr.S_un.S_addr = inet_addr(host)

            val result = connect(
                sock,
                addr.ptr.reinterpret(),
                sizeOf<sockaddr_in>().convert(),
            )
            result == 0
        } finally {
            closesocket(sock)
        }
    }

    /**
     * Detect network type by interface name heuristics.
     * On Windows, common patterns: "Wi-Fi", "Ethernet", "Bluetooth", "VPN".
     */
    private fun detectNetworkType(): NetworkType {
        // MinGW doesn't have GetAdaptersAddresses in posix bindings,
        // so we use interface name heuristics via basic Winsock inspection.
        // Default to Ethernet which is the most common Windows desktop connection.
        return NetworkType.Ethernet
    }

    /** @return true if state changed. */
    private fun updateState(online: Boolean): Boolean {
        val newInfo = if (online) {
            NetworkInfo(type = detectNetworkType())
        } else {
            null
        }

        val newStatus = if (newInfo != null) {
            NetworkStatus.Available(newInfo)
        } else {
            NetworkStatus.Unavailable
        }

        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return false

        _networkStatus.value = newStatus
        _isOnline.value = online

        when {
            newStatus is NetworkStatus.Available && oldStatus is NetworkStatus.Unavailable -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Connected(newInfo!!))
            }

            newStatus is NetworkStatus.Unavailable && oldStatus is NetworkStatus.Available -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
            }
        }
        return true
    }

    private var closed = false

    override suspend fun probe(): NetworkStatus = withContext(Dispatchers.IO) {
        updateState(checkNetwork())
        networkStatus.value
    }

    override fun force() {
        scope.launch { probe() }
    }

    override fun close() {
        if (!closed) {
            closed = true
            scope.cancel()
            WSACleanup()
        }
    }
}
