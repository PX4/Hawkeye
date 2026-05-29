package com.px4.hawkeye.feature.settings.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ThemeResolutionTest {
    @Test fun `LIGHT is always light regardless of system`() {
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(systemInDark = true)).isEqualTo(false)
        assertThat(ThemeMode.LIGHT.resolveDarkTheme(systemInDark = false)).isEqualTo(false)
    }
    @Test fun `DARK is always dark regardless of system`() {
        assertThat(ThemeMode.DARK.resolveDarkTheme(systemInDark = false)).isEqualTo(true)
        assertThat(ThemeMode.DARK.resolveDarkTheme(systemInDark = true)).isEqualTo(true)
    }
    @Test fun `SYSTEM follows the system setting`() {
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(systemInDark = true)).isEqualTo(true)
        assertThat(ThemeMode.SYSTEM.resolveDarkTheme(systemInDark = false)).isEqualTo(false)
    }
}
