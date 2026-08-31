package com.px4.hawkeye.core.navigation.di

import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shell-level navigation DI seam. Empty today; reserved for future shell-wide
 * navigation singletons.
 *
 * Note: feature [EntryProviderInstaller]s are NOT registered here. A feature whose
 * destination needs nothing from the shell may register itself in its own presentation
 * module with `single<EntryProviderInstaller>(named("<feature>")) { ... }`, which the
 * shell aggregates via Koin's `getAll<EntryProviderInstaller>()`. A destination that
 * needs the back stack (for a back button, or to push another destination) is registered
 * directly in `:app`'s AppShell instead, which is where every current destination lives.
 */
val navigationModule: Module = module { }
