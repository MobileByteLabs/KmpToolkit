package com.mobilebytelabs.producttickets.data.remote

import com.mobilebytelabs.producttickets.config.ProductTicketsConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

internal object ProductTicketsClient {
    val instance: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = ProductTicketsConfig.supabaseUrl,
            supabaseKey = ProductTicketsConfig.supabaseAnonKey,
        ) {
            install(Postgrest)
        }
    }
}
