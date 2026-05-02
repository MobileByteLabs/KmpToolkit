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
 * Attempts a TCP connect to Google DNS (8.8.8.8:53) to verify actual
 * internet connectivity. Polls at [NetworkMonitorConfig.pollIntervalMs].
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

    init {
        initWinsock()
        updateState(checkNetwork())
        scope.launch {
            while (isActive) {
                delay(config.pollIntervalMs)
                updateState(checkNetwork())
            }
        }
    }

    private fun initWinsock() = memScoped {
        val wsaData = alloc<WSADATA>()
        WSAStartup(0x0202.convert(), wsaData.ptr)
    }

    /**
     * Attempt a TCP connect to 8.8.8.8:53 (Google DNS).
     * If connect succeeds, we have internet. Fast and reliable.
     */
    private fun checkNetwork(): Boolean = memScoped {
        val sock: SOCKET = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
        if (sock == INVALID_SOCKET) return false

        try {
            val addr = alloc<sockaddr_in>()
            addr.sin_family = AF_INET.convert()
            addr.sin_port = htons(53.toShort()).toUShort()
            addr.sin_addr.S_un.S_addr = inet_addr("8.8.8.8")

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

    private fun updateState(online: Boolean) {
        val newInfo = if (online) {
            NetworkInfo(type = NetworkType.Ethernet)
        } else {
            null
        }

        val newStatus = if (newInfo != null) {
            NetworkStatus.Available(newInfo)
        } else {
            NetworkStatus.Unavailable
        }

        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return

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
    }

    override fun close() {
        scope.cancel()
        WSACleanup()
    }
}
