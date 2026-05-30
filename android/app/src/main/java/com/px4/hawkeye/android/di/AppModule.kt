package com.px4.hawkeye.android.di

import com.px4.hawkeye.android.shell.ShellViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** App-level DI: shell-scoped ViewModels that aren't owned by a feature module. */
val appModule = module {
    viewModelOf(::ShellViewModel)
}
