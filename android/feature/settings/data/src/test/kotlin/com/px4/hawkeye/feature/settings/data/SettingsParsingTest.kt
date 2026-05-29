package com.px4.hawkeye.feature.settings.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.px4.hawkeye.feature.settings.domain.DistanceUnit
import com.px4.hawkeye.feature.settings.domain.ThemeMode
import org.junit.jupiter.api.Test

class SettingsParsingTest {

    // --- parseThemeMode ---

    @Test
    fun `parseThemeMode with valid name SYSTEM returns SYSTEM`() {
        assertThat(parseThemeMode("SYSTEM")).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `parseThemeMode with valid name LIGHT returns LIGHT`() {
        assertThat(parseThemeMode("LIGHT")).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `parseThemeMode with valid name DARK returns DARK`() {
        assertThat(parseThemeMode("DARK")).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `parseThemeMode with null returns default SYSTEM`() {
        assertThat(parseThemeMode(null)).isEqualTo(ThemeMode.SYSTEM)
    }

    @Test
    fun `parseThemeMode with unknown string returns default SYSTEM`() {
        assertThat(parseThemeMode("NONSENSE")).isEqualTo(ThemeMode.SYSTEM)
    }

    // --- parseDistanceUnit ---

    @Test
    fun `parseDistanceUnit with valid name METRIC returns METRIC`() {
        assertThat(parseDistanceUnit("METRIC")).isEqualTo(DistanceUnit.METRIC)
    }

    @Test
    fun `parseDistanceUnit with valid name IMPERIAL returns IMPERIAL`() {
        assertThat(parseDistanceUnit("IMPERIAL")).isEqualTo(DistanceUnit.IMPERIAL)
    }

    @Test
    fun `parseDistanceUnit with null returns default METRIC`() {
        assertThat(parseDistanceUnit(null)).isEqualTo(DistanceUnit.METRIC)
    }

    @Test
    fun `parseDistanceUnit with unknown string returns default METRIC`() {
        assertThat(parseDistanceUnit("NONSENSE")).isEqualTo(DistanceUnit.METRIC)
    }
}
