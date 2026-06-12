package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class WheelMenuStateTest {

    private fun items(count: Int = 6) = List(count) { index ->
        WheelMenuItem(label = "Item $index", accentColor = Color.White)
    }

    /** A state sized like a landscape phone with easy round numbers. */
    private fun state() = WheelMenuState(items()).apply {
        outerRadiusPx = 300f
        deadZoneRadiusPx = 100f
        bounds = Size(2000f, 1000f)
    }

    @Test
    fun `starts closed with no hover`() {
        val state = state()
        assertThat(state.isOpen).isFalse()
        assertThat(state.hoveredIndex).isNull()
    }

    @Test
    fun `open clamps the center fully on screen`() {
        val state = state()
        state.open(Offset(100f, 500f))
        assertThat(state.isOpen).isTrue()
        assertThat(state.center).isEqualTo(Offset(300f, 500f))
    }

    @Test
    fun `open without known bounds keeps the raw center`() {
        val state = WheelMenuState(items()).apply {
            outerRadiusPx = 300f
            deadZoneRadiusPx = 100f
        }
        state.open(Offset(100f, 500f))
        assertThat(state.center).isEqualTo(Offset(100f, 500f))
    }

    @Test
    fun `move resolves the hovered slice against the clamped center`() {
        val state = state()
        state.open(Offset(100f, 500f)) // clamps to x = 300
        state.move(Offset(700f, 500f)) // right of the clamped center: slice 1 of 6
        assertThat(state.hoveredIndex).isEqualTo(1)
    }

    @Test
    fun `move inside the hub dead zone hovers nothing`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1010f, 510f))
        assertThat(state.hoveredIndex).isNull()
    }

    @Test
    fun `move while closed is a no-op`() {
        val state = state()
        state.move(Offset(700f, 500f))
        assertThat(state.hoveredIndex).isNull()
        assertThat(state.isOpen).isFalse()
    }

    @Test
    fun `release returns the hovered index and closes`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f)) // slice 1
        assertThat(state.release()).isEqualTo(1)
        assertThat(state.isOpen).isFalse()
        assertThat(state.hoveredIndex).isNull()
    }

    @Test
    fun `release is one-shot`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f))
        state.release()
        assertThat(state.release()).isNull()
    }

    @Test
    fun `release in the dead zone selects nothing`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1010f, 505f))
        assertThat(state.release()).isNull()
        assertThat(state.isOpen).isFalse()
    }

    @Test
    fun `release while closed returns nothing`() {
        val state = state()
        assertThat(state.release()).isNull()
    }

    @Test
    fun `cancel closes without selection`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f))
        state.cancel()
        assertThat(state.isOpen).isFalse()
        assertThat(state.release()).isNull()
    }

    @Test
    fun `reopening after release starts with a clean hover`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f))
        state.release()
        state.open(Offset(1000f, 500f))
        assertThat(state.hoveredIndex).isNull()
    }

    @Test
    fun `items can be swapped at runtime`() {
        val state = state()
        state.items = items(count = 4)
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f)) // 3 o'clock with 4 slices: slice 1
        assertThat(state.hoveredIndex).isEqualTo(1)
    }

    @Test
    fun `move into the upper left quadrant hovers the last slice`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(800f, 300f)) // between 9 and 12 o'clock: slice 5 of 6
        assertThat(state.hoveredIndex).isEqualTo(5)
    }

    @Test
    fun `swapping items while open clears the hover and blocks a stale release`() {
        val state = state()
        state.open(Offset(1000f, 500f))
        state.move(Offset(800f, 300f)) // slice 5 of 6
        state.items = items(count = 2) // index 5 no longer exists
        assertThat(state.hoveredIndex).isNull()
        assertThat(state.release()).isNull()
    }

    @Test
    fun `move never hovers before the hit geometry is synced`() {
        // deadZoneRadiusPx left at 0: everything is dead zone until WheelMenu syncs it.
        val state = WheelMenuState(items())
        state.open(Offset(1000f, 500f))
        state.move(Offset(1400f, 500f))
        assertThat(state.hoveredIndex).isNull()
    }
}
