package com.px4.hawkeye.feature.replay.presentation.di

import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import com.px4.hawkeye.core.navigation.ReplayKey
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryRoot
import com.px4.hawkeye.feature.replay.presentation.ReplayLibraryViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val replayPresentationModule = module {
    viewModelOf(::ReplayLibraryViewModel)
    single<EntryProviderInstaller>(named("replay")) {
        { addEntryProvider(ReplayKey::class, { it.toString() }) { ReplayLibraryRoot() } }
    }
}
