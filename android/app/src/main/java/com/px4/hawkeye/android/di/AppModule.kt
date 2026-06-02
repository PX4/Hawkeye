package com.px4.hawkeye.android.di

import com.px4.hawkeye.android.live.AndroidLivePlaybackLauncher
import com.px4.hawkeye.android.replay.AndroidReplayPlaybackLauncher
import com.px4.hawkeye.android.shell.ShellViewModel
import com.px4.hawkeye.core.presentation.LivePlaybackLauncher
import com.px4.hawkeye.core.presentation.ReplayPlaybackLauncher
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** App-level DI: shell-scoped ViewModels and seams that wire features to the renderer. */
val appModule = module {
    viewModelOf(::ShellViewModel)
    single<ReplayPlaybackLauncher> { AndroidReplayPlaybackLauncher() }
    single<LivePlaybackLauncher> { AndroidLivePlaybackLauncher(get()) }
}
