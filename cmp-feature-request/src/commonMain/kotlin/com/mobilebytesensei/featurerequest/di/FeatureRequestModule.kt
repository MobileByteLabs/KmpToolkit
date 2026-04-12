package com.mobilebytesensei.featurerequest.di

import com.mobilebytesensei.featurerequest.data.UserTicketsRepository
import com.mobilebytesensei.featurerequest.data.UserTicketsService
import com.mobilebytesensei.featurerequest.data.UserTicketsServiceImpl
import com.mobilebytesensei.featurerequest.ui.UserTicketsViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val featureRequestModule = module {
    singleOf(::UserTicketsServiceImpl) bind UserTicketsService::class
    singleOf(::UserTicketsRepository)
    viewModelOf(::UserTicketsViewModel)
}
