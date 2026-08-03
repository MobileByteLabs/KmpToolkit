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

    private val _monitoring = MutableStateFlow(false)
    override val monitoring: StateFlow<Boolean> = _monitoring.asStateFlow()

    init {
        if (config.autoStart) start()
    }

    override fun start() {
        if (closed || _monitoring.value) return
        // Apply initial validation if strategy requires HTTP.
        if (window.navigator.onLine && config.validationStrategy != ValidationStrategy.NativeOnly) {
            scope.launch {
                if (!httpHeadCheck()) updateOffline()
            }
        }
        window.addEventListener("online", onlineHandler)
        window.addEventListener("offline", offlineHandler)
        _monitoring.value = true
    }

    override fun stop() {
        if (!_monitoring.value) return
        _monitoring.value = false
        window.removeEventListener("online", onlineHandler)
        window.removeEventListener("offline", offlineHandler)
    }

    // Any-success over the primary + fallback endpoints (sequential; short-circuits on first hit).
    // NOTE: cross-origin generate_204 HEADs may be CORS-opaque (status 0) in the browser — on JS the
    // reliable signal is navigator.onLine (NativeOnly); HTTP validation is best-effort here.
    private suspend fun httpHeadCheck(): Boolean {
        for (url in config.effectiveValidationUrls) {
            if (probeUrl(url)) return true
        }
        return false
    }

    private suspend fun probeUrl(url: String): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val xhr = XMLHttpRequest()
            xhr.open(config.validationMethod, url)
            xhr.timeout = config.validationTimeoutMs.toInt()
            xhr.onload = {
                // 204 sentinel; a captive portal returns 200 with a login page → NOT validated.
                val code = xhr.status.toInt()
                val ok = code == 204
                config.onValidationResult?.invoke(ValidationResult(url, ok, code, -1L))
                if (cont.isActive) cont.resume(ok)
            }
            xhr.onerror = {
                config.onValidationResult?.invoke(ValidationResult(url, false, null, -1L))
                if (cont.isActive) cont.resume(false)
            }
            xhr.ontimeout = {
                config.onValidationResult?.invoke(ValidationResult(url, false, null, -1L))
                if (cont.isActive) cont.resume(false)
            }
            cont.invokeOnCancellation { xhr.abort() }
            xhr.send()
        } catch (_: Throwable) {
            config.onValidationResult?.invoke(ValidationResult(url, false, null, -1L))
            if (cont.isActive) cont.resume(false)
        }
    }

    override suspend fun probe(): NetworkStatus {
        if (!window.navigator.onLine) {
            updateOffline()
            return _networkStatus.value
        }
        val info = detectNetworkInfo()
        val online = when (config.validationStrategy) {
            ValidationStrategy.NativeOnly -> true
            ValidationStrategy.HttpOnly, ValidationStrategy.NativeThenHttp -> httpHeadCheck()
        }
        if (online) updateOnline(info) else updateOffline()
        return _networkStatus.value
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

    override fun force() {
        scope.launch { probe() }
    }

    override fun close() {
        if (!closed) {
            closed = true
            // M-001 fix: remove listeners (via stop) BEFORE cancelling the scope. The reverse
            // order created a window where an event firing between scope.cancel()
            // and removeEventListener would invoke a handler against a cancelled scope.
            stop()
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
