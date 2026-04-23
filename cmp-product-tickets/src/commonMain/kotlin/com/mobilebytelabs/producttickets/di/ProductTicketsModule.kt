package com.mobilebytelabs.producttickets.di

import com.mobilebytelabs.producttickets.data.remote.ProductTicketsRepository
import com.mobilebytelabs.producttickets.data.remote.ProductTicketsService
import com.mobilebytelabs.producttickets.data.remote.ProductTicketsServiceImpl
import com.mobilebytelabs.producttickets.ui.ProductTicketsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val productTicketsModule = module {
    singleOf(::ProductTicketsServiceImpl) bind ProductTicketsService::class
    singleOf(::ProductTicketsRepository)
    viewModelOf(::ProductTicketsViewModel)
}
