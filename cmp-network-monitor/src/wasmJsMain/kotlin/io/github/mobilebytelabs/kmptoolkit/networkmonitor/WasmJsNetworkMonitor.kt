package io.github.mobilebytelabs.kmptoolkit.networkmonitor

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
import kotlin.js.ExperimentalWasmJsInterop

/**
 * External JS interop for WasmJS browser APIs.
 * Uses `@OptIn(ExperimentalWasmJsInterop::class)
@JsFun` to inline JS code for navigator and event listener access.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private external interface JsEventHandler : JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => typeof globalThis.navigator !== 'undefined' ? globalThis.navigator.onLine : true")
private external fun jsIsOnline(): Boolean

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(fn) => { const h = () => fn(); globalThis.addEventListener('online', h); return h; }")
private external fun jsRegisterOnline(fn: () -> Unit): JsEventHandler

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(h) => globalThis.removeEventListener('online', h)")
private external fun jsUnregisterOnline(h: JsEventHandler)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(fn) => { const h = () => fn(); globalThis.addEventListener('offline', h); return h; }")
private external fun jsRegisterOffline(fn: () -> Unit): JsEventHandler

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(h) => globalThis.removeEventListener('offline', h)")
private external fun jsUnregisterOffline(h: JsEventHandler)

/**
 * WasmJS [NetworkMonitor] backed by `navigator.onLine` + online/offline events.
 *
 * Uses WasmJS-specific `@OptIn(ExperimentalWasmJsInterop::class)
@JsFun` interop for browser API access.
 * HTTP validation is not supported on WasmJS due to limited browser API interop;
 * [ValidationStrategy] settings are acknowledged but NativeOnly behavior is used.
 */
internal class WasmJsNetworkMonitor(private val config: NetworkMonitorConfig) : NetworkMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isOnline = MutableStateFlow(jsIsOnline())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _networkStatus = MutableStateFlow(currentStatus())
    override val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _networkChanges = MutableSharedFlow<NetworkChangeEvent>(extraBufferCapacity = 64)
    override val networkChanges: SharedFlow<NetworkChangeEvent> = _networkChanges.asSharedFlow()

    private val onlineJsHandler: JsEventHandler = jsRegisterOnline {
        val oldStatus = _networkStatus.value
        val info = NetworkInfo(type = NetworkType.Unknown)
        _isOnline.value = true
        _networkStatus.value = NetworkStatus.Available(info)
        if (oldStatus is NetworkStatus.Unavailable) {
            _networkChanges.tryEmit(NetworkChangeEvent.Connected(info))
        }
    }

    private val offlineJsHandler: JsEventHandler = jsRegisterOffline {
        val wasOnline = _isOnline.value
        _isOnline.value = false
        _networkStatus.value = NetworkStatus.Unavailable
        if (wasOnline) {
            _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
        }
    }

    override fun close() {
        scope.cancel()
        jsUnregisterOnline(onlineJsHandler)
        jsUnregisterOffline(offlineJsHandler)
    }

    private fun currentStatus(): NetworkStatus = if (jsIsOnline()) {
        NetworkStatus.Available(NetworkInfo(type = NetworkType.Unknown))
    } else {
        NetworkStatus.Unavailable
    }
}
