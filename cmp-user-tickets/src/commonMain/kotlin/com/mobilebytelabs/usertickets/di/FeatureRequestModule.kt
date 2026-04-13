package com.mobilebytelabs.usertickets.di

import com.mobilebytelabs.usertickets.data.UserTicketsRepository
import com.mobilebytelabs.usertickets.data.UserTicketsService
import com.mobilebytelabs.usertickets.data.UserTicketsServiceImpl
import com.mobilebytelabs.usertickets.ui.UserTicketsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureRequestModule = module {
    singleOf(::UserTicketsServiceImpl) bind UserTicketsService::class
    singleOf(::UserTicketsRepository)
    viewModelOf(::UserTicketsViewModel)
}
