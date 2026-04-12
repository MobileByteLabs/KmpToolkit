package com.mobilebytesensei.usertickets.data

import com.mobilebytesensei.usertickets.config.FeatureRequestConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

internal object FeatureRequestClient {
    val instance: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = FeatureRequestConfig.supabaseUrl,
            supabaseKey = FeatureRequestConfig.supabaseAnonKey,
        ) {
            install(Postgrest)
        }
    }
}
