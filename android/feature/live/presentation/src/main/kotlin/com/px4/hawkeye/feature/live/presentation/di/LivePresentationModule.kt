package com.px4.hawkeye.feature.live.presentation.di

import com.px4.hawkeye.feature.live.presentation.LiveSetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val livePresentationModule = module {
    // Parameterized: the shell passes the listen port (read from settings) at navigation time.
    viewModel { (listenPort: Int) -> LiveSetupViewModel(get(), listenPort) }
}
