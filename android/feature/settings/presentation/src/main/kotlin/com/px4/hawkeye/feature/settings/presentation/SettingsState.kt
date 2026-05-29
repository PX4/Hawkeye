package com.px4.hawkeye.feature.settings.presentation

import com.px4.hawkeye.feature.settings.domain.DistanceUnit
import com.px4.hawkeye.feature.settings.domain.ThemeMode

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
)
