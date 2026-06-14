package com.px4.hawkeye.core.designsystem

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class DronePaletteTest {

    @Test
    fun `palette has sixteen distinct colors`() {
        assertThat(HawkeyeDronePalette.colors).hasSize(16)
        assertThat(HawkeyeDronePalette.colors.distinct()).hasSize(16)
    }

    @Test
    fun `palette matches the native default theme anchors`() {
        // Spot-check against src/theme.c default theme drone_palette to catch transcription
        // drift: index 0 (primary white), 1 (blue), 5 (orange), 15 (lime).
        assertThat(HawkeyeDronePalette.colors[0]).isEqualTo(Color(230, 230, 230))
        assertThat(HawkeyeDronePalette.colors[1]).isEqualTo(Color(40, 120, 255))
        assertThat(HawkeyeDronePalette.colors[5]).isEqualTo(Color(255, 140, 0))
        assertThat(HawkeyeDronePalette.colors[15]).isEqualTo(Color(200, 255, 60))
    }
}
