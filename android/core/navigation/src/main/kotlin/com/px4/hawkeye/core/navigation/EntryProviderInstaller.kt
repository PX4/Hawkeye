package com.px4.hawkeye.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/**
 * A feature contributes its NavEntry mappings by registering one of these in Koin.
 * The shell collects all installers and applies them to a single entryProvider.
 */
typealias EntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit
