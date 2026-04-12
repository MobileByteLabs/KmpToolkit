package com.mobilebytesensei.usertickets.di

import com.mobilebytesensei.usertickets.data.UserTicketsRepository
import com.mobilebytesensei.usertickets.data.UserTicketsService
import com.mobilebytesensei.usertickets.data.UserTicketsServiceImpl
import com.mobilebytesensei.usertickets.ui.UserTicketsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureRequestModule = module {
    singleOf(::UserTicketsServiceImpl) bind UserTicketsService::class
    singleOf(::UserTicketsRepository)
    viewModelOf(::UserTicketsViewModel)
}
