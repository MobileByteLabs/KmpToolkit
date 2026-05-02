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
        val info = NetworkInfo(type = NetworkType.Unknown)
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

    private val offlineHandler: (Event) -> Unit = {
        updateOffline()
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

    override fun close() {
        scope.cancel()
        window.removeEventListener("online", onlineHandler)
        window.removeEventListener("offline", offlineHandler)
    }

    private fun currentJsStatus(): NetworkStatus = if (window.navigator.onLine) {
        NetworkStatus.Available(NetworkInfo(type = NetworkType.Unknown))
    } else {
        NetworkStatus.Unavailable
    }
}
