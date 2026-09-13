package com.mobilebytelabs.remoteconfig.network

import co.touchlab.kermit.Logger
import com.mobilebytelabs.remoteconfig.cmpMetadata
import com.mobilebytelabs.remoteconfig.model.DeviceImpression
import com.mobilebytelabs.remoteconfig.model.RemoteConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val TAG = "RemoteConfigService"
private const val TABLE = "product_remote_config"

class RemoteConfigService(private val supabaseUrl: String, private val supabaseKey: String) {
    private val client: SupabaseClient by lazy {
        createSupabaseClient(supabaseUrl = supabaseUrl, supabaseKey = supabaseKey) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                },
            )
            install(Postgrest)
        }
    }

    suspend fun getActiveConfigs(): List<RemoteConfig> = try {
        client.postgrest[TABLE]
            .select {
                filter {
                    eq("is_enabled", true)
                }
                order("priority", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<RemoteConfig>()
            .also { report("configs_fetched", mapOf("count" to it.size)) }
    } catch (e: Exception) {
        Logger.e(TAG) { "Failed to fetch configs: ${e.message}" }
        // Worth reporting precisely BECAUSE this returns emptyList(): to the caller a fetch
        // failure and a genuinely empty config set look identical, so without this event a
        // remote-config outage is invisible. Exception CLASS only, never the message — a
        // Postgrest error message can echo the query.
        report("configs_fetch_failed", mapOf("error" to e::class.simpleName))
        emptyList()
    }

    suspend fun getDeviceImpressions(deviceId: String): List<DeviceImpression> = try {
        client.postgrest.rpc(
            function = "get_device_impressions",
            parameters = buildJsonObject {
                put("p_device_id", deviceId)
            },
        ).decodeList<DeviceImpression>()
    } catch (e: Exception) {
        Logger.e(TAG) { "Failed to get impressions: ${e.message}" }
        emptyList()
    }

    suspend fun recordImpression(configId: String, deviceId: String) {
        try {
            client.postgrest.rpc(
                function = "record_config_impression",
                parameters = buildJsonObject {
                    put("p_config_id", configId)
                    put("p_device_id", deviceId)
                },
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Failed to record impression: ${e.message}" }
        }
    }

    suspend fun dismissConfig(configId: String, deviceId: String) {
        try {
            client.postgrest.rpc(
                function = "dismiss_config",
                parameters = buildJsonObject {
                    put("p_config_id", configId)
                    put("p_device_id", deviceId)
                },
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Failed to dismiss config: ${e.message}" }
        }
    }

    /**
     * Reports config-pipeline shape only.
     *
     * Never the config ids, payloads or device id: a remote config body IS the unreleased product
     * decision, and the device id is a stable identifier.
     */
    private fun report(event: String, payload: Map<String, Any?>) {
        observeLifecycle(cmpMetadata(), event, payload)
    }
}
