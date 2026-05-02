package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
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
import platform.posix.closedir
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fcntl
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.freeaddrinfo
import platform.posix.getaddrinfo
import platform.posix.getsockopt
import platform.posix.opendir
import platform.posix.poll
import platform.posix.pollfd
import platform.posix.readdir
import platform.posix.socket

/**
 * Linux [NetworkMonitor] backed by `/sys/class/net` polling.
 *
 * Dynamically enumerates all network interfaces via `/sys/class/net/`,
 * reads each interface's `operstate` file, and skips `lo` (loopback).
 * Detects WiFi vs Ethernet by interface naming convention (wl* = wireless).
 * Polls at [NetworkMonitorConfig.pollIntervalMs].
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Only checks `/sys/class/net` interface state.
 * - [ValidationStrategy.HttpOnly]: TCP connect to validation URL host.
 * - [ValidationStrategy.NativeThenHttp]: Interface check first, then TCP connect to confirm.
 */
@OptIn(ExperimentalForeignApi::class)
internal class LinuxNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

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
        updateState(checkNetwork())
        scope.launch {
            while (isActive) {
                delay(adaptivePolling.intervalMs)
                val changed = updateState(checkNetwork())
                if (changed) adaptivePolling.reportChange() else adaptivePolling.reportStable()
            }
        }
    }

    /**
     * Check network based on configured [ValidationStrategy].
     */
    private fun checkNetwork(): NetworkInfo? {
        return when (config.validationStrategy) {
            ValidationStrategy.NativeOnly -> checkNativeNetwork()

            ValidationStrategy.HttpOnly -> {
                if (tcpConnectCheck()) {
                    checkNativeNetwork() ?: NetworkInfo(type = NetworkType.Unknown)
                } else {
                    null
                }
            }

            ValidationStrategy.NativeThenHttp -> {
                val info = checkNativeNetwork() ?: return null
                if (tcpConnectCheck()) info else null
            }
        }
    }

    /**
     * Enumerate all interfaces in /sys/class/net/, skip loopback,
     * check operstate for "up", and determine the network type.
     */
    private fun checkNativeNetwork(): NetworkInfo? {
        val interfaces = listNetworkInterfaces()
        for (iface in interfaces) {
            if (iface == "lo") continue // skip loopback
            val operstate = readFile("/sys/class/net/$iface/operstate")?.trim()
            if (operstate == "up" || operstate == "unknown") {
                val type = detectInterfaceType(iface)
                val speedMbps = readFile("/sys/class/net/$iface/speed")?.trim()?.toIntOrNull()
                val downstreamKbps = if (speedMbps != null && speedMbps > 0) speedMbps * 1000 else -1
                return NetworkInfo(
                    type = type,
                    isMetered = type == NetworkType.Cellular,
                    downstreamBandwidthKbps = downstreamKbps,
                    upstreamBandwidthKbps = downstreamKbps, // symmetric on Linux
                )
            }
        }
        return null
    }

    /**
     * TCP connect to validation URL host with non-blocking socket + poll() timeout.
     * Same pattern as Apple's tcpReachabilityCheck().
     */
    @Suppress("ReturnCount")
    private fun tcpConnectCheck(): Boolean = memScoped {
        val host = config.validationUrl
            .removePrefix("https://").removePrefix("http://")
            .substringBefore("/").substringBefore(":")
        val port = if (config.validationUrl.startsWith("https://")) "443" else "80"

        val hints = alloc<addrinfo>()
        hints.ai_family = AF_INET
        hints.ai_socktype = SOCK_STREAM
        hints.ai_protocol = IPPROTO_TCP

        val result = allocPointerTo<addrinfo>()
        if (getaddrinfo(host, port, hints.ptr, result.ptr) != 0) return false

        val addrInfo = result.value ?: return false
        try {
            val sock = socket(
                addrInfo.pointed.ai_family,
                addrInfo.pointed.ai_socktype,
                addrInfo.pointed.ai_protocol,
            )
            if (sock < 0) return false

            try {
                // Set non-blocking
                val flags = fcntl(sock, F_GETFL, 0)
                if (fcntl(sock, F_SETFL, flags or O_NONBLOCK) < 0) return false

                val connectResult = platform.posix.connect(
                    sock,
                    addrInfo.pointed.ai_addr,
                    addrInfo.pointed.ai_addrlen,
                )
                if (connectResult == 0) return true
                if (errno != EINPROGRESS) return false

                // Wait with poll()
                val pfd = alloc<pollfd>()
                pfd.fd = sock
                pfd.events = POLLOUT.convert()

                val pollResult = poll(pfd.ptr, 1u, config.validationTimeoutMs.toInt())
                if (pollResult <= 0) return false

                // Verify connect succeeded
                val optVal = alloc<IntVar>()
                val optLen = alloc<UIntVar>()
                optLen.value = sizeOf<IntVar>().convert()
                getsockopt(
                    sock,
                    SOL_SOCKET,
                    SO_ERROR,
                    optVal.ptr,
                    optLen.ptr,
                )
                optVal.value == 0
            } finally {
                platform.posix.close(sock)
            }
        } finally {
            freeaddrinfo(addrInfo)
        }
    }

    /** List all entries in /sys/class/net/ directory. */
    private fun listNetworkInterfaces(): List<String> {
        val interfaces = mutableListOf<String>()
        val dir = opendir("/sys/class/net") ?: return interfaces
        try {
            while (true) {
                val entry = readdir(dir) ?: break
                val name = entry.pointed.d_name.toKString()
                if (name != "." && name != "..") {
                    interfaces.add(name)
                }
            }
        } finally {
            closedir(dir)
        }
        return interfaces
    }

    /**
     * Detect [NetworkType] from Linux interface naming conventions:
     * - wl*, wlan* = WiFi
     * - eth*, en*, em* = Ethernet
     * - ww*, ppp* = Cellular/mobile
     * - tun*, tap* = VPN
     * - br*, docker*, veth* = Virtual/bridge (treated as Ethernet)
     */
    private fun detectInterfaceType(name: String): NetworkType = when {
        name.startsWith("wl") || name.startsWith("wlan") -> NetworkType.WiFi
        name.startsWith("ww") || name.startsWith("ppp") -> NetworkType.Cellular
        name.startsWith("tun") || name.startsWith("tap") -> NetworkType.VPN
        name.startsWith("eth") || name.startsWith("en") || name.startsWith("em") -> NetworkType.Ethernet
        else -> NetworkType.Ethernet // default for unknown interfaces
    }

    private fun readFile(path: String): String? = memScoped {
        val file: CPointer<platform.posix.FILE> = fopen(path, "r") ?: return null
        val buffer = allocArray<ByteVar>(256)
        val result = fgets(buffer, 256, file)
        fclose(file)
        result?.toKString()
    }

    /** @return true if state changed. */
    private fun updateState(newInfo: NetworkInfo?): Boolean {
        val newStatus = if (newInfo != null) {
            NetworkStatus.Available(newInfo)
        } else {
            NetworkStatus.Unavailable
        }

        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return false

        _networkStatus.value = newStatus
        _isOnline.value = newInfo != null

        when {
            newStatus is NetworkStatus.Available && oldStatus is NetworkStatus.Unavailable -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Connected(newInfo!!))
            }

            newStatus is NetworkStatus.Unavailable && oldStatus is NetworkStatus.Available -> {
                _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
            }

            newStatus is NetworkStatus.Available && oldStatus is NetworkStatus.Available -> {
                if (newInfo!!.type != oldStatus.info.type) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.TypeChanged(oldStatus.info.type, newInfo.type),
                    )
                }
            }
        }
        return true
    }

    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            scope.cancel()
        }
    }
}
