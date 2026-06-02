package com.px4.hawkeye.feature.settings.domain

import com.px4.hawkeye.core.domain.DEFAULT_LIVE_PORT

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val listenPort: Int = DEFAULT_LIVE_PORT,
)
