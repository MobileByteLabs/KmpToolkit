package com.mobilebytesensei.remoteconfig.di

import com.mobilebytesensei.usertickets.config.FeatureRequestConfig
import com.mobilebytesensei.remoteconfig.RemoteConfigEvaluator
import com.mobilebytesensei.remoteconfig.local.DeviceIdProvider
import com.mobilebytesensei.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytesensei.remoteconfig.network.RemoteConfigService
import com.mobilebytesensei.remoteconfig.ui.RemoteConfigViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val remoteConfigModule = module {
    single {
        RemoteConfigService(
            supabaseUrl = FeatureRequestConfig.supabaseUrl,
            supabaseKey = FeatureRequestConfig.supabaseAnonKey,
            productType = FeatureRequestConfig.productType,
        )
    }
    singleOf(::RemoteConfigLocalStore)
    singleOf(::DeviceIdProvider)
    singleOf(::RemoteConfigEvaluator)
    viewModelOf(::RemoteConfigViewModel)
}
