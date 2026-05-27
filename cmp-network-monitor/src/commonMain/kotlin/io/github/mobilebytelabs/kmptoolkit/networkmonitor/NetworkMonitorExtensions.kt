package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Throws [NetworkUnavailableException] if currently offline.
 * Use at function entry to fail-fast on network-dependent operations.
 */
fun NetworkMonitor.requireOnline() {
    if (!isOnline.value) {
        throw NetworkUnavailableException()
    }
}

/**
 * Suspends until the device is online, then returns.
 * Returns immediately if already online.
 */
suspend fun NetworkMonitor.ensureOnline() {
    if (!isOnline.value) {
        isOnline.first { it }
    }
}

/**
 * Suspends until online, executes [block], and returns the result.
 * Throws [NetworkUnavailableException] if cancelled while waiting.
 */
suspend fun <T> NetworkMonitor.withNetworkGuard(block: suspend () -> T): T {
    ensureOnline()
    return block()
}

/**
 * Suspends until the device is online and returns the [NetworkInfo].
 * Returns immediately if already online.
 */
suspend fun NetworkMonitor.awaitOnline(): NetworkInfo {
    val status = networkStatus.first { it is NetworkStatus.Available }
    return (status as NetworkStatus.Available).info
}

/**
 * Returns a [kotlinx.coroutines.flow.Flow] that mirrors [networkStatus]
 * but completes when the device goes offline.
 */
fun NetworkMonitor.onlyWhileOnline() = networkStatus.transformWhile { status ->
    if (status is NetworkStatus.Available) {
        emit(status)
        true
    } else {
        false
    }
}

/**
 * Retries [action] each time the device reconnects after a failure.
 * Returns the first successful result.
 *
 * @param maxRetries Maximum number of retries (default 3). Use [Int.MAX_VALUE] for indefinite.
 * @param timeoutMs Overall timeout in milliseconds (default [Long.MAX_VALUE] = no timeout).
 * @param action The suspending action to retry.
 * @throws NetworkUnavailableException if [maxRetries] is exhausted or [timeoutMs] elapses.
 */
suspend fun <T> NetworkMonitor.retryOnReconnect(
    maxRetries: Int = 3,
    timeoutMs: Long = Long.MAX_VALUE,
    action: suspend () -> T,
): T {
    val result = withTimeoutOrNull(timeoutMs) {
        var lastException: Throwable? = null
        repeat(maxRetries) {
            try {
                ensureOnline()
                return@withTimeoutOrNull action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                // Wait for next reconnect event
                isOnline.first { !it } // wait to go offline
                isOnline.first { it } // wait to come back online
            }
        }
        throw NetworkUnavailableException(
            message = "Failed after $maxRetries retries",
            cause = lastException,
        )
    }
    return result ?: throw NetworkUnavailableException(
        message = "Retry timed out after ${timeoutMs}ms",
    )
}

/**
 * Returns `true` if currently connected via the specified [NetworkType].
 */
fun NetworkMonitor.isConnectedVia(type: NetworkType): Boolean {
    val status = networkStatus.value
    return status is NetworkStatus.Available && status.info.type == type
}

/**
 * Debounced online state flow that filters out rapid WiFi<->Cellular handoff flicker.
 *
 * @param timeoutMs Debounce window in milliseconds. Default 300ms handles most handoffs.
 */
@OptIn(FlowPreview::class)
fun NetworkMonitor.isOnlineDebounced(timeoutMs: Long = 300L): Flow<Boolean> =
    isOnline.debounce(timeoutMs).distinctUntilChanged()

/**
 * Debounced network status flow.
 *
 * @param timeoutMs Debounce window in milliseconds.
 */
@OptIn(FlowPreview::class)
fun NetworkMonitor.debouncedNetworkStatus(timeoutMs: Long = 300L): Flow<NetworkStatus> =
    networkStatus.debounce(timeoutMs).distinctUntilChanged()

/**
 * Debounced online state as a hot [StateFlow] seeded with the current [isOnline] value.
 *
 * Unlike [isOnlineDebounced] (cold [Flow]), late subscribers immediately observe the
 * currently-settled value via [StateFlow.value]. The flow filters out rapid WiFi <->
 * Cellular handoff flicker via [debounce] + [distinctUntilChanged].
 *
 * The hot flow is shared eagerly while [scope] is active; callers MUST cancel [scope]
 * (or pass a lifecycle-bound scope such as `viewModelScope`) to avoid leaks.
 *
 * @param scope Coroutine scope that hosts the shared upstream collector.
 * @param timeoutMs Debounce window in milliseconds. Default 300ms handles most handoffs.
 */
@OptIn(FlowPreview::class)
fun NetworkMonitor.isOnlineDebouncedState(scope: CoroutineScope, timeoutMs: Long = 300L): StateFlow<Boolean> = isOnline
    .debounce(timeoutMs)
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, isOnline.value)

/**
 * Debounced network status as a hot [StateFlow] seeded with the current [networkStatus] value.
 *
 * Same semantics as [isOnlineDebouncedState] but exposes the full [NetworkStatus]
 * (Available / Unavailable / CaptivePortal).
 *
 * @param scope Coroutine scope that hosts the shared upstream collector.
 * @param timeoutMs Debounce window in milliseconds.
 */
@OptIn(FlowPreview::class)
fun NetworkMonitor.networkStatusDebouncedState(
    scope: CoroutineScope,
    timeoutMs: Long = 300L,
): StateFlow<NetworkStatus> = networkStatus
    .debounce(timeoutMs)
    .distinctUntilChanged()
    .stateIn(scope, SharingStarted.Eagerly, networkStatus.value)

/**
 * Flow of [NetworkQuality] derived from current network status.
 * Emits only when quality level changes.
 */
fun NetworkMonitor.networkQuality(): Flow<NetworkQuality> = networkStatus.map { it.toQuality() }.distinctUntilChanged()

/**
 * Current [NetworkQuality] snapshot.
 */
val NetworkMonitor.currentQuality: NetworkQuality
    get() = networkStatus.value.toQuality()

/**
 * Combine a data flow with network state. Emits [Pair] of the latest value
 * from [this] flow and the current [NetworkStatus].
 *
 * Useful for UI layers that need to show data + connectivity state together.
 *
 * ```kotlin
 * dataFlow.withNetworkState(monitor).collect { (data, status) ->
 *     if (status.isOnline) showData(data) else showOfflineBanner(data)
 * }
 * ```
 */
fun <T> Flow<T>.withNetworkState(monitor: NetworkMonitor): Flow<Pair<T, NetworkStatus>> =
    combine(this, monitor.networkStatus) { data, status ->
        data to status
    }

/**
 * Filter a flow to only emit values when the device is online.
 *
 * Values emitted while offline are dropped. When the device comes back online,
 * the next value from the upstream flow will be emitted.
 */
fun <T> Flow<T>.onlyWhileOnline(monitor: NetworkMonitor): Flow<T> = combine(this, monitor.isOnline) { value, online ->
    value to online
}.mapNotNull { (value, online) -> if (online) value else null }

/**
 * Suspends until the monitor reports the specified [NetworkType], then returns the [NetworkInfo].
 */
suspend fun NetworkMonitor.awaitConnectionType(type: NetworkType): NetworkInfo {
    val status = networkStatus.first { s ->
        s is NetworkStatus.Available && s.info.type == type
    }
    return (status as NetworkStatus.Available).info
}

/**
 * Suspend-safe close that ensures cleanup runs on the appropriate dispatcher.
 * Returns immediately if the monitor is already closed.
 */
suspend fun NetworkMonitor.closeGracefully() {
    withContext(Dispatchers.Default) {
        close()
    }
}

/**
 * Execute [block] if currently online, returning the result. Returns null if offline.
 *
 * ```kotlin
 * val data = monitor.ifOnline { api.fetchData() }
 * ```
 */
inline fun <T> NetworkMonitor.ifOnline(block: (NetworkInfo) -> T): T? {
    val status = networkStatus.value
    return if (status is NetworkStatus.Available) block(status.info) else null
}

/**
 * Execute [block] if currently offline, returning the result. Returns null if online.
 *
 * ```kotlin
 * monitor.ifOffline { showCachedData() }
 * ```
 */
inline fun <T> NetworkMonitor.ifOffline(block: () -> T): T? = if (!isOnline.value) block() else null

/**
 * Measure HTTP round-trip latency to the validation URL.
 *
 * @return Round-trip time in milliseconds, or -1 if the check failed.
 */
suspend fun NetworkMonitor.measureLatency(): Long {
    if (!isOnline.value) return -1L
    val start = currentTimeMillis()
    return try {
        val result = platformDetectCaptivePortal(NetworkMonitorConfig())
        if (result is CaptivePortalResult.NoCaptivePortal) {
            currentTimeMillis() - start
        } else {
            -1L
        }
    } catch (_: Exception) {
        -1L
    }
}

/**
 * Register callbacks for network state changes. Returns a [CallbackHandle] that
 * must be [closed][CallbackHandle.close] to stop receiving callbacks.
 *
 * ```kotlin
 * val handle = monitor.addCallback(
 *     onOnline = { info -> log("Online: ${info.type}") },
 *     onOffline = { log("Offline") },
 * )
 * // Later:
 * handle.close()
 * ```
 */
fun NetworkMonitor.addCallback(
    scope: CoroutineScope,
    onOnline: (NetworkInfo) -> Unit = {},
    onOffline: () -> Unit = {},
): CallbackHandle {
    val collectorJob = CoroutineScope(
        (scope.coroutineContext[Job] ?: error("CoroutineScope must have a Job")) + Dispatchers.Default,
    ).launch {
        networkStatus.collect { status ->
            when (status) {
                is NetworkStatus.Available -> onOnline(status.info)
                is NetworkStatus.CaptivePortal -> onOffline()
                is NetworkStatus.Unavailable -> onOffline()
            }
        }
    }

    return CallbackHandle { collectorJob.cancel() }
}

/**
 * Handle returned by [addCallback]. Call [close] to stop receiving callbacks.
 */
fun interface CallbackHandle {
    fun close()
}
