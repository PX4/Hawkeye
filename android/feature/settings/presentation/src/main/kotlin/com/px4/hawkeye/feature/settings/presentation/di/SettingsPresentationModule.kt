package com.px4.hawkeye.feature.settings.presentation.di

import com.px4.hawkeye.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Settings has no EntryProviderInstaller: it navigates to About, so its entry needs the
// shell's back stack and is registered in :app alongside Home, Replay, and Live.
val settingsPresentationModule = module {
    viewModelOf(::SettingsViewModel)
}
