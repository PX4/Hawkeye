package com.px4.hawkeye.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Top-level destinations shown in the navigation suite (bar/rail). */
enum class TopLevelDestination(val key: NavKey) {
    HOME(HomeKey),
    SETTINGS(SettingsKey),
}

@Serializable data object HomeKey : NavKey
@Serializable data object ReplayKey : NavKey
@Serializable data object LiveKey : NavKey
@Serializable data object SettingsKey : NavKey
@Serializable data object AboutKey : NavKey
