package com.px4.hawkeye.feature.replay.presentation.di

import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// The ReplayKey NavEntry is registered in :app's AppShell (like Home/Live) so it can wire the
// top-bar back button to the shell's back stack; this module just provides the ViewModel.
val replayPresentationModule = module {
    viewModelOf(::ReplayLibraryViewModel)
}
