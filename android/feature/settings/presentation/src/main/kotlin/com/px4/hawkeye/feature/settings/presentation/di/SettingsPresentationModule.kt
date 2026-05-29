package com.px4.hawkeye.feature.settings.presentation.di

import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import com.px4.hawkeye.core.navigation.SettingsKey
import com.px4.hawkeye.feature.settings.presentation.SettingsRoot
import com.px4.hawkeye.feature.settings.presentation.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsPresentationModule = module {
    viewModelOf(::SettingsViewModel)
    single<EntryProviderInstaller>(named("settings")) {
        { addEntryProvider(SettingsKey::class, { it.toString() }) { SettingsRoot() } }
    }
}
