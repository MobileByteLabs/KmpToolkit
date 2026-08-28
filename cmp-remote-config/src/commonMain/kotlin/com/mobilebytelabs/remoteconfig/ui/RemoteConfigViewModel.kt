package com.mobilebytelabs.remoteconfig.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobilebytelabs.remoteconfig.RemoteConfigEvaluator
import com.mobilebytelabs.remoteconfig.local.DeviceIdProvider
import com.mobilebytelabs.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytelabs.remoteconfig.model.DeviceImpression
import com.mobilebytelabs.remoteconfig.model.RemoteConfig
import com.mobilebytelabs.remoteconfig.network.RemoteConfigService
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class RemoteConfigState(val activeConfig: RemoteConfig? = null, val isLoading: Boolean = true)

class RemoteConfigViewModel(
    private val service: RemoteConfigService,
    private val evaluator: RemoteConfigEvaluator,
    private val localStore: RemoteConfigLocalStore,
    private val deviceIdProvider: DeviceIdProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(RemoteConfigState())
    val state: StateFlow<RemoteConfigState> = _state.asStateFlow()

    private val deviceId: String by lazy { deviceIdProvider.getDeviceId() }

    /**
     * @param appVersion the host app's current version name (e.g. "2026.8.4"). When supplied it is
     *   passed to the evaluator so a config's `min_app_version` / `max_app_version` window is honored.
     *   Null (the default) falls back to the DI-provided version supplier (`remoteConfig { appVersion
     *   = … }`); if neither is set, version-bounded configs are excluded (fail-closed).
     */
    fun fetchAndEvaluate(appVersion: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Hard timeout so a slow/blocked network can't hang the dialog.
            val configs = withTimeoutOrNull(FETCH_TIMEOUT_MS) { service.getActiveConfigs() }
            if (configs == null) {
                // Slow/blocked network — fall back to the last-good cached config, but
                // ONLY if it still passes the SAME eligibility rules (impression cap /
                // cooldown / dismissed / version window). This never re-shows a cooldown-gated
                // dialog (e.g. a once-per-day "Rate Us"); the cache is a network fallback, not
                // a bypass of the server rules.
                val cachedEligible = localStore.getCachedConfig()
                    ?.let { evaluator.evaluate(listOf(it), appVersion = appVersion) }
                _state.update { it.copy(activeConfig = cachedEligible ?: it.activeConfig, isLoading = false) }
                return@launch
            }

            // Fetch server impressions (survives reinstall), also bounded.
            val serverImpressions = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                try {
                    service.getDeviceImpressions(deviceId).associateBy { it.configId }
                } catch (_: Exception) {
                    emptyMap<String, DeviceImpression>()
                }
            } ?: emptyMap()

            // The evaluator is authoritative — it applies the impression/cooldown/dismiss
            // rules. Cache the eligible result only as a network fallback for next time.
            val active = evaluator.evaluate(configs, serverImpressions, appVersion = appVersion)
            active?.let { localStore.cacheConfig(it) }
            _state.update { it.copy(activeConfig = active, isLoading = false) }
        }
    }

    private companion object {
        const val FETCH_TIMEOUT_MS = 2500L
    }

    fun onConfigShown(configId: String) {
        val now = GMTDate().timestamp
        localStore.incrementImpressions(configId, now)

        // Record on server (fire-and-forget, survives reinstall)
        viewModelScope.launch {
            service.recordImpression(configId, deviceId)
        }
    }

    fun onConfigDismissed(configId: String, permanent: Boolean = false) {
        if (permanent) {
            localStore.markDismissed(configId)
            // Persist dismiss on server (survives reinstall)
            viewModelScope.launch {
                service.dismissConfig(configId, deviceId)
            }
        }
        _state.update { it.copy(activeConfig = null) }
    }

    fun onActionClicked(configId: String) {
        val now = GMTDate().timestamp
        localStore.incrementImpressions(configId, now)
        viewModelScope.launch {
            service.recordImpression(configId, deviceId)
        }
        _state.update { it.copy(activeConfig = null) }
    }
}
