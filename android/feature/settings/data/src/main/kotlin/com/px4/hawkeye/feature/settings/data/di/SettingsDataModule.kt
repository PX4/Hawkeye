package com.px4.hawkeye.feature.settings.data.di

import com.px4.hawkeye.feature.settings.data.DataStoreSettingsRepository
import com.px4.hawkeye.feature.settings.domain.SettingsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val settingsDataModule = module {
    single<SettingsRepository> { DataStoreSettingsRepository(androidContext()) }
}
