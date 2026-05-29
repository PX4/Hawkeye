package com.px4.hawkeye.feature.home.presentation.di

import com.px4.hawkeye.feature.home.presentation.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homePresentationModule = module {
    viewModelOf(::HomeViewModel)
}
