package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URI

/**
 * JVM [NetworkMonitor] backed by polling + HTTP validation.
 *
 * Since JVM has no push-based network change API, this implementation
 * polls at [NetworkMonitorConfig.pollIntervalMs] intervals. Uses
 * [NetworkInterface] enumeration for native check and HTTP HEAD
 * for validation (based on [NetworkMonitorConfig.validationStrategy]).
 */
internal class JvmNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isOnline = MutableStateFlow(false)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow<NetworkStatus>(NetworkStatus.Unavailable)
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    init {
        // Seed initial state synchronously
        updateState(checkNetwork())

        // Start polling loop
        scope.launch {
            while (isActive) {
                delay(config.pollIntervalMs)
                val info = checkNetwork()
                updateState(info)
            }
        }
    }

    private fun checkNetwork(): NetworkInfo? {
        return try {
            val hasInterface = hasActiveNetworkInterface()
            if (!hasInterface) return null

            when (config.validationStrategy) {
                ValidationStrategy.NativeOnly -> {
                    if (hasInterface) defaultNetworkInfo() else null
                }

                ValidationStrategy.HttpOnly -> {
                    if (httpHeadCheck()) defaultNetworkInfo() else null
                }

                ValidationStrategy.NativeThenHttp -> {
                    if (hasInterface && httpHeadCheck()) defaultNetworkInfo() else null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hasActiveNetworkInterface(): Boolean = try {
        NetworkInterface.getNetworkInterfaces()?.toList()?.any { iface ->
            iface.isUp && !iface.isLoopback && iface.inetAddresses.hasMoreElements()
        } ?: false
    } catch (_: Exception) {
        false
    }

    private fun httpHeadCheck(): Boolean = try {
        val connection = URI(config.validationUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connectTimeout = config.validationTimeoutMs.toInt()
        connection.readTimeout = config.validationTimeoutMs.toInt()
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        try {
            val code = connection.responseCode
            code in 200..399
        } finally {
            connection.disconnect()
        }
    } catch (_: Exception) {
        false
    }

    private fun defaultNetworkInfo(): NetworkInfo {
        // JVM can't easily distinguish WiFi/Cellular/Ethernet at the Java level
        // Use Ethernet as the most common JVM network type
        return NetworkInfo(
            type = NetworkType.Ethernet,
            isMetered = false,
        )
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
                if (newInfo.isMetered != oldStatus.info.isMetered) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.MeteredChanged(newInfo.isMetered),
                    )
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
