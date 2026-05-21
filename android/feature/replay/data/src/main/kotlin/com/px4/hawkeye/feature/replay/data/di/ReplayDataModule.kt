package com.px4.hawkeye.feature.replay.data.di

import com.px4.hawkeye.feature.replay.data.AndroidUlogInboxDataSource
import com.px4.hawkeye.feature.replay.domain.UlogInboxDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val replayDataModule = module {
    single<UlogInboxDataSource> {
        AndroidUlogInboxDataSource(
            contentResolver = androidContext().contentResolver,
            filesDir = androidContext().filesDir
        )
    }
}
