package com.px4.hawkeye.feature.settings.domain

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
)
