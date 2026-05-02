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

    private val adaptivePolling = AdaptivePollingState(
        baseIntervalMs = config.pollIntervalMs,
        maxIntervalMs = config.backgroundPollIntervalMs,
    )

    private val validationBackoff = ValidationBackoffState(
        maxDelayMs = config.maxValidationBackoffMs,
    )

    init {
        // Seed from disk cache for fast cold-start, then update with live check
        val cached = loadCachedNetworkState()
        if (cached != null && cached.wasOnline &&
            (currentTimeMillis() - cached.timestampMs) < CachedNetworkState.MAX_AGE_MS
        ) {
            updateState(cached.toNetworkInfo())
        }

        // Seed initial state synchronously (overrides cache if different)
        updateState(checkNetwork())

        // Start adaptive polling loop
        scope.launch {
            while (isActive) {
                delay(adaptivePolling.intervalMs)
                val info = checkNetwork()
                val changed = updateState(info)
                if (changed) {
                    adaptivePolling.reportChange()
                    // Persist to disk cache on state change
                    saveCachedNetworkState(CachedNetworkState.from(_networkStatus.value) ?: continue)
                } else {
                    adaptivePolling.reportStable()
                }
            }
        }
    }

    private fun checkNetwork(): NetworkInfo? {
        return try {
            val hasInterface = hasActiveNetworkInterface()
            if (!hasInterface) return null

            when (config.validationStrategy) {
                ValidationStrategy.NativeOnly -> {
                    if (hasInterface) detectNetworkInfo() else null
                }

                ValidationStrategy.HttpOnly -> {
                    if (httpHeadCheck()) detectNetworkInfo() else null
                }

                ValidationStrategy.NativeThenHttp -> {
                    if (hasInterface && httpHeadCheck()) detectNetworkInfo() else null
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

    private fun httpHeadCheck(): Boolean {
        if (validationBackoff.shouldBackoff) {
            // Backoff active — skip this cycle but decrement
            validationBackoff.reportSuccess() // gradually recover
            return _isOnline.value // maintain current state
        }

        return try {
            val connection = URI(config.validationUrl).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = config.validationTimeoutMs.toInt()
            connection.readTimeout = config.validationTimeoutMs.toInt()
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            try {
                val code = connection.responseCode
                val success = code in 200..399
                if (success) validationBackoff.reportSuccess() else validationBackoff.reportFailure()
                success
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            validationBackoff.reportFailure()
            false
        }
    }

    private fun detectNetworkInfo(): NetworkInfo {
        try {
            val activeInterfaces = NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { it.isUp && !it.isLoopback && it.inetAddresses.hasMoreElements() }
                ?: return NetworkInfo(type = NetworkType.Ethernet, isMetered = false)

            for (iface in activeInterfaces) {
                val name = iface.name.lowercase()
                val type = when {
                    name.startsWith("wl") || name.startsWith("wlan") || name.contains("wi-fi") ||
                        name.contains("wifi") -> NetworkType.WiFi

                    name.startsWith("ww") || name.startsWith("ppp") || name.startsWith("rmnet") ->
                        NetworkType.Cellular

                    name.startsWith("tun") || name.startsWith("tap") -> NetworkType.VPN

                    name.startsWith("eth") || name.startsWith("en") || name.startsWith("em") ->
                        NetworkType.Ethernet

                    else -> null
                }
                if (type != null) {
                    return NetworkInfo(
                        type = type,
                        isMetered = type == NetworkType.Cellular,
                    )
                }
            }
        } catch (_: Exception) {
            // Fall through to default
        }
        return NetworkInfo(type = NetworkType.Ethernet, isMetered = false)
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
                if (newInfo.isMetered != oldStatus.info.isMetered) {
                    _networkChanges.tryEmit(
                        NetworkChangeEvent.MeteredChanged(newInfo.isMetered),
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
