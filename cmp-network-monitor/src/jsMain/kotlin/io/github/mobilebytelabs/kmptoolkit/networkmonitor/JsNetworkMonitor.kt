package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.browser.window
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.events.Event
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

/**
 * JS [NetworkMonitor] backed by `navigator.onLine` + online/offline events.
 *
 * This is push-based — no polling needed. The browser fires `online`
 * and `offline` events on the `window` object when connectivity changes.
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Uses only navigator.onLine (default).
 * - [ValidationStrategy.HttpOnly]: Validates with XMLHttpRequest HEAD before reporting online.
 * - [ValidationStrategy.NativeThenHttp]: Reports online immediately, then validates with HTTP HEAD.
 *
 * Note: HTTP validation may be limited by CORS policy depending on [NetworkMonitorConfig.validationUrl].
 */
internal class JsNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isOnline = MutableStateFlow(window.navigator.onLine)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow(currentJsStatus())
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private val onlineHandler: (Event) -> Unit = {
        // Defense-in-depth (M-001): even if removeEventListener races, a fired
        // handler after close() must not touch the cancelled scope.
        if (!closed && scope.isActive) {
            val info = detectNetworkInfo()
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
    }

    private val offlineHandler: (Event) -> Unit = {
        if (!closed && scope.isActive) {
            updateOffline()
        }
    }

    init {
        // Apply initial validation if strategy requires HTTP
        if (window.navigator.onLine && config.validationStrategy != ValidationStrategy.NativeOnly) {
            scope.launch {
                if (!httpHeadCheck()) updateOffline()
            }
        }
        window.addEventListener("online", onlineHandler)
        window.addEventListener("offline", offlineHandler)
    }

    private suspend fun httpHeadCheck(): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val xhr = XMLHttpRequest()
            xhr.open("HEAD", config.validationUrl)
            xhr.timeout = config.validationTimeoutMs.toInt()
            xhr.onload = {
                if (cont.isActive) cont.resume(xhr.status.toInt() in 200..399)
            }
            xhr.onerror = {
                if (cont.isActive) cont.resume(false)
            }
            xhr.ontimeout = {
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation { xhr.abort() }
            xhr.send()
        } catch (_: Throwable) {
            if (cont.isActive) cont.resume(false)
        }
    }

    private fun updateOnline(info: NetworkInfo) {
        val newStatus = NetworkStatus.Available(info)
        val oldStatus = _networkStatus.value
        if (newStatus == oldStatus) return

        _networkStatus.value = newStatus
        _isOnline.value = true

        if (oldStatus is NetworkStatus.Unavailable) {
            _networkChanges.tryEmit(NetworkChangeEvent.Connected(info))
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

    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            // M-001 fix: remove listeners BEFORE cancelling the scope. The reverse
            // order created a window where an event firing between scope.cancel()
            // and removeEventListener would invoke a handler against a cancelled scope.
            window.removeEventListener("online", onlineHandler)
            window.removeEventListener("offline", offlineHandler)
            scope.cancel()
        }
    }

    /**
     * Detect network info using the Network Information API when available.
     * Falls back to [NetworkType.Unknown] if `navigator.connection` is not supported.
     */
    private fun detectNetworkInfo(): NetworkInfo {
        try {
            val connection = js("navigator.connection || navigator.mozConnection || navigator.webkitConnection")
            if (connection != null && connection != undefined) {
                val effectiveType = connection.effectiveType as? String
                val downlink = (connection.downlink as? Number)?.toDouble() ?: -1.0

                val type = when (effectiveType) {
                    "4g" -> NetworkType.WiFi

                    // 4g effective = likely WiFi or fast cellular
                    "3g" -> NetworkType.Cellular

                    "2g", "slow-2g" -> NetworkType.Cellular

                    else -> NetworkType.Unknown
                }

                val downstreamKbps = if (downlink > 0) (downlink * 1000).toInt() else -1

                return NetworkInfo(
                    type = type,
                    isMetered = connection.saveData as? Boolean ?: false,
                    downstreamBandwidthKbps = downstreamKbps,
                )
            }
        } catch (_: Throwable) {
            // Network Information API not available
        }
        return NetworkInfo(type = NetworkType.Unknown)
    }

    private fun currentJsStatus(): NetworkStatus = if (window.navigator.onLine) {
        NetworkStatus.Available(detectNetworkInfo())
    } else {
        NetworkStatus.Unavailable
    }
}
