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
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(url, timeoutMs, onSuccess, onFailure) => {
    try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        fetch(url, { method: 'HEAD', mode: 'no-cors', signal: controller.signal })
            .then(() => { clearTimeout(timer); onSuccess(); })
            .catch(() => { clearTimeout(timer); onFailure(); });
    } catch(e) { onFailure(); }
}""",
)
private external fun jsFetchHead(url: JsString, timeoutMs: JsNumber, onSuccess: () -> Unit, onFailure: () -> Unit)

/**
 * WasmJS [NetworkMonitor] backed by `navigator.onLine` + online/offline events.
 *
 * Uses WasmJS-specific `@JsFun` interop for browser API access.
 *
 * Supports [ValidationStrategy]:
 * - [ValidationStrategy.NativeOnly]: Uses only navigator.onLine (default).
 * - [ValidationStrategy.HttpOnly]: Validates with fetch() HEAD before reporting online.
 * - [ValidationStrategy.NativeThenHttp]: Reports online immediately, then validates with fetch().
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
        val info = NetworkInfo(type = NetworkType.Unknown)
        handleNativeOnline(info)
    }

    private val offlineJsHandler: JsEventHandler = jsRegisterOffline {
        updateOffline()
    }

    init {
        // Apply initial HTTP validation if strategy requires it
        if (jsIsOnline() && config.validationStrategy != ValidationStrategy.NativeOnly) {
            scope.launch {
                fetchHeadCheck { success ->
                    if (!success) updateOffline()
                }
            }
        }
    }

    private fun handleNativeOnline(info: NetworkInfo) {
        when (config.validationStrategy) {
            ValidationStrategy.NativeOnly -> updateOnline(info)

            ValidationStrategy.HttpOnly -> {
                fetchHeadCheck { success ->
                    if (success) updateOnline(info) else updateOffline()
                }
            }

            ValidationStrategy.NativeThenHttp -> {
                updateOnline(info) // optimistic
                fetchHeadCheck { success ->
                    if (!success) updateOffline()
                }
            }
        }
    }

    private fun updateOnline(info: NetworkInfo) {
        val oldStatus = _networkStatus.value
        val newStatus = NetworkStatus.Available(info)
        if (newStatus == oldStatus) return

        _isOnline.value = true
        _networkStatus.value = newStatus
        if (oldStatus is NetworkStatus.Unavailable) {
            _networkChanges.tryEmit(NetworkChangeEvent.Connected(info))
        }
    }

    private fun updateOffline() {
        val wasOnline = _isOnline.value
        _isOnline.value = false
        _networkStatus.value = NetworkStatus.Unavailable
        if (wasOnline) {
            _networkChanges.tryEmit(NetworkChangeEvent.Disconnected)
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    private fun fetchHeadCheck(callback: (Boolean) -> Unit) {
        jsFetchHead(
            config.validationUrl.toJsString(),
            config.validationTimeoutMs.toInt().toJsNumber(),
            onSuccess = { callback(true) },
            onFailure = { callback(false) },
        )
    }

    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            scope.cancel()
            jsUnregisterOnline(onlineJsHandler)
            jsUnregisterOffline(offlineJsHandler)
        }
    }

    private fun currentStatus(): NetworkStatus = if (jsIsOnline()) {
        NetworkStatus.Available(NetworkInfo(type = NetworkType.Unknown))
    } else {
        NetworkStatus.Unavailable
    }
}
