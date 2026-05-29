package com.px4.hawkeye.android

import android.app.Application
import com.px4.hawkeye.core.navigation.di.navigationModule
import com.px4.hawkeye.feature.home.presentation.di.homePresentationModule
import com.px4.hawkeye.feature.replay.data.di.replayDataModule
import com.px4.hawkeye.feature.replay.presentation.di.replayPresentationModule
import com.px4.hawkeye.feature.settings.data.di.settingsDataModule
import com.px4.hawkeye.feature.settings.presentation.di.settingsPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HawkeyeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HawkeyeApp)
            modules(
                navigationModule,
                homePresentationModule,
                settingsDataModule,
                settingsPresentationModule,
                replayDataModule,
                replayPresentationModule,
            )
        }
    }
}
