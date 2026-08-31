package com.px4.hawkeye.feature.about.presentation.di

import com.px4.hawkeye.feature.about.presentation.AboutViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val aboutPresentationModule = module {
    viewModelOf(::AboutViewModel)
}
