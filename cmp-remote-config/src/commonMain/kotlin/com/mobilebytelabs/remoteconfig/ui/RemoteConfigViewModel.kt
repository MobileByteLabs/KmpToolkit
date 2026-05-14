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

    fun fetchAndEvaluate() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val configs = service.getActiveConfigs()

            // Fetch server impressions (survives reinstall)
            val serverImpressions = try {
                service.getDeviceImpressions(deviceId)
                    .associateBy { it.configId }
            } catch (_: Exception) {
                emptyMap<String, DeviceImpression>()
            }

            val active = evaluator.evaluate(configs, serverImpressions)
            _state.update { it.copy(activeConfig = active, isLoading = false) }
        }
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
