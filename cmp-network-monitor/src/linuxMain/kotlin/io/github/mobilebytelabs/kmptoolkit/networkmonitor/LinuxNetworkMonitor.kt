package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
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
import platform.posix.closedir
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.opendir
import platform.posix.readdir

/**
 * Linux [NetworkMonitor] backed by `/sys/class/net` polling.
 *
 * Dynamically enumerates all network interfaces via `/sys/class/net/`,
 * reads each interface's `operstate` file, and skips `lo` (loopback).
 * Detects WiFi vs Ethernet by interface naming convention (wl* = wireless).
 * Polls at [NetworkMonitorConfig.pollIntervalMs].
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

    init {
        updateState(checkNetwork())
        scope.launch {
            while (isActive) {
                delay(config.pollIntervalMs)
                updateState(checkNetwork())
            }
        }
    }

    /**
     * Enumerate all interfaces in /sys/class/net/, skip loopback,
     * check operstate for "up", and determine the network type.
     */
    private fun checkNetwork(): NetworkInfo? {
        val interfaces = listNetworkInterfaces()
        for (iface in interfaces) {
            if (iface == "lo") continue // skip loopback
            val operstate = readFile("/sys/class/net/$iface/operstate")?.trim()
            if (operstate == "up" || operstate == "unknown") {
                val type = detectInterfaceType(iface)
                return NetworkInfo(type = type, isMetered = type == NetworkType.Cellular)
            }
        }
        return null
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

    private fun updateState(newInfo: NetworkInfo?) {
        val newStatus = if (newInfo != null) {
            NetworkStatus.Available(newInfo)
        } else {
            NetworkStatus.Unavailable
        }

        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return

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
    }

    override fun close() {
        scope.cancel()
    }
}
