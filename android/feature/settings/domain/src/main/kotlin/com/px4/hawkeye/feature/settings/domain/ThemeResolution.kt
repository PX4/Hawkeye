package com.px4.hawkeye.feature.settings.domain

/** Resolves the effective dark-theme flag for a [ThemeMode], given the current system setting. */
fun ThemeMode.resolveDarkTheme(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
