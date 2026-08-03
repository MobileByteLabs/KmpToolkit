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
import kotlinx.coroutines.isActive
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
    """(url, method, timeoutMs, onResult) => {
    try {
        const controller = new AbortController();
        const timer = setTimeout(() => controller.abort(), timeoutMs);
        // cors + redirect:manual so we can READ the status (204 sentinel) for same-origin /
        // CORS-enabled endpoints and a captive-portal redirect isn't silently followed.
        fetch(url, { method: method, mode: 'cors', redirect: 'manual', cache: 'no-store', signal: controller.signal })
            .then((r) => { clearTimeout(timer); onResult(r.status); })
            .catch(() => {
                clearTimeout(timer);
                // Cross-origin without CORS headers (e.g. google generate_204) -> opaque; fall back
                // to no-cors reachability where the status is unreadable (-1 = reachable-but-opaque).
                fetch(url, { method: method, mode: 'no-cors', cache: 'no-store' })
                    .then(() => onResult(-1))
                    .catch(() => onResult(0));
            });
    } catch(e) { onResult(0); }
}""",
)
private external fun jsFetchStatus(url: JsString, method: JsString, timeoutMs: JsNumber, onResult: (Int) -> Unit)

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
        // Defense-in-depth (M-001 mirror): post-close events must not touch
        // the cancelled scope. closed-flag + scope.isActive double-check.
        if (!closed && scope.isActive) {
            val info = NetworkInfo(type = NetworkType.Unknown)
            handleNativeOnline(info)
        }
    }

    private val offlineJsHandler: JsEventHandler = jsRegisterOffline {
        if (!closed && scope.isActive) {
            updateOffline()
        }
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

    /**
     * HTTP validation via `fetch`. `204` from a generate_204 endpoint = verified real internet
     * (and correctly rejects a captive portal, whose 200/redirect is not 204). NOTE: reading the
     * status cross-origin is CORS-limited — a same-origin `validationUrl` (e.g. the app backend's
     * `/generate_204`) is required for true 204 detection on the web; otherwise the probe falls
     * back to opaque reachability (`-1`, treated as online) so cross-origin defaults don't regress.
     */
    @OptIn(ExperimentalWasmJsInterop::class)
    private fun fetchHeadCheck(callback: (Boolean) -> Unit) {
        jsFetchStatus(
            config.validationUrl.toJsString(),
            config.validationMethod.toJsString(),
            config.validationTimeoutMs.toInt().toJsNumber(),
        ) { status ->
            // 204 = verified; -1 = opaque-reachable (cross-origin fallback, can't verify → online);
            // anything else (portal 200, error 0) = not validated.
            val ok = status == 204 || status == -1
            config.onValidationResult?.invoke(
                ValidationResult(config.validationUrl, ok, status.takeIf { it >= 0 }, -1L),
            )
            callback(ok)
        }
    }

    override suspend fun probe(): NetworkStatus {
        // Re-read navigator.onLine now (the reliable WasmJS signal) and re-sync the flows.
        _networkStatus.value = currentStatus()
        _isOnline.value = jsIsOnline()
        return networkStatus.value
    }

    override fun force() {
        scope.launch { probe() }
    }

    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            // M-001 mirror: unregister listeners FIRST, then cancel the scope.
            // The reverse order created a window where a JS event firing between
            // scope.cancel() and unregister would invoke a handler on the cancelled scope.
            jsUnregisterOnline(onlineJsHandler)
            jsUnregisterOffline(offlineJsHandler)
            scope.cancel()
        }
    }

    private fun currentStatus(): NetworkStatus = if (jsIsOnline()) {
        NetworkStatus.Available(NetworkInfo(type = NetworkType.Unknown))
    } else {
        NetworkStatus.Unavailable
    }
}
