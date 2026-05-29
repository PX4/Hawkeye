package com.px4.hawkeye.core.navigation.di

import com.px4.hawkeye.core.navigation.EntryProviderInstaller
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Shell-level navigation DI seam. Empty today; reserved for future shell-wide
 * navigation singletons.
 *
 * Note: feature [EntryProviderInstaller]s are NOT registered here. Each feature
 * registers its own in its own presentation module with
 * `single<EntryProviderInstaller>(named("<feature>")) { ... }`; the shell then
 * aggregates them across all loaded modules via Koin's `getAll<EntryProviderInstaller>()`.
 */
val navigationModule: Module = module { }
