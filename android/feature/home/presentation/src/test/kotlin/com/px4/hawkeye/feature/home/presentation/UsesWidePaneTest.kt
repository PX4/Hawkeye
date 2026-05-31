package com.px4.hawkeye.feature.home.presentation

import androidx.window.core.layout.WindowSizeClass
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class UsesWidePaneTest {

    @Test
    fun `width just below the medium breakpoint stays single column`() {
        assertThat(usesWidePane(WindowSizeClass(minWidthDp = 599, minHeightDp = 480))).isFalse()
    }

    @Test
    fun `width at the medium breakpoint switches to wide pane`() {
        assertThat(usesWidePane(WindowSizeClass(minWidthDp = 600, minHeightDp = 480))).isTrue()
    }

    @Test
    fun `width above the medium breakpoint stays wide pane`() {
        assertThat(usesWidePane(WindowSizeClass(minWidthDp = 601, minHeightDp = 480))).isTrue()
    }
}
