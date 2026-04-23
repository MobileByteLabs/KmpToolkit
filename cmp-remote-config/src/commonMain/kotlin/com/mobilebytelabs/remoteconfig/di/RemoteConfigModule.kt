package com.mobilebytelabs.remoteconfig.di

import com.mobilebytelabs.producttickets.config.ProductTicketsConfig
import com.mobilebytelabs.remoteconfig.RemoteConfigEvaluator
import com.mobilebytelabs.remoteconfig.local.DeviceIdProvider
import com.mobilebytelabs.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytelabs.remoteconfig.network.RemoteConfigService
import com.mobilebytelabs.remoteconfig.ui.RemoteConfigViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val remoteConfigModule = module {
    single {
        RemoteConfigService(
            supabaseUrl = ProductTicketsConfig.supabaseUrl,
            supabaseKey = ProductTicketsConfig.supabaseAnonKey,
            productType = ProductTicketsConfig.boardType,
        )
    }
    singleOf(::RemoteConfigLocalStore)
    singleOf(::DeviceIdProvider)
    singleOf(::RemoteConfigEvaluator)
    viewModelOf(::RemoteConfigViewModel)
}
