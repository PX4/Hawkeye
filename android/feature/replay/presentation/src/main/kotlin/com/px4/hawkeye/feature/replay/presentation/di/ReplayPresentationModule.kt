package com.px4.hawkeye.feature.replay.presentation.di

import com.px4.hawkeye.feature.replay.presentation.ReplayViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val replayPresentationModule = module {
    viewModelOf(::ReplayViewModel)
}
