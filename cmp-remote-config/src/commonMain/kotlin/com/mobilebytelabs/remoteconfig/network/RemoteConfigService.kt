package com.mobilebytelabs.remoteconfig.network

import co.touchlab.kermit.Logger
import com.mobilebytelabs.remoteconfig.model.DeviceImpression
import com.mobilebytelabs.remoteconfig.model.RemoteConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
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
    } catch (e: Exception) {
        Logger.e(TAG) { "Failed to fetch configs: ${e.message}" }
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
}
