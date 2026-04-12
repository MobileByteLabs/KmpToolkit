package com.mobilebytesensei.featurerequest.data

import com.mobilebytesensei.featurerequest.config.FeatureRequestConfig
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
