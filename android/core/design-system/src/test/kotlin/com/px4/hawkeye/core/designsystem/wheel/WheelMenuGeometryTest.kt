package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.ui.geometry.Offset
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class WheelMenuGeometryTest {

    private val center = Offset(500f, 500f)

    @Test
    fun `finger just right of twelve o'clock maps to slice 0 of 6`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(510f, 300f), 6, 50f)
        assertThat(idx).isEqualTo(0)
    }

    @Test
    fun `finger at three o'clock maps to slice 1 of 6`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(700f, 500f), 6, 50f)
        assertThat(idx).isEqualTo(1)
    }

    @Test
    fun `finger at six o'clock maps to slice 3 of 6`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(500f, 700f), 6, 50f)
        assertThat(idx).isEqualTo(3)
    }

    @Test
    fun `finger at nine o'clock maps to slice 4 of 6`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(300f, 500f), 6, 50f)
        assertThat(idx).isEqualTo(4)
    }

    @Test
    fun `finger just left of twelve o'clock maps to slice 5 of 6`() {
        // Wrap-around region: angles just shy of 12 o'clock belong to the last slice.
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(490f, 300f), 6, 50f)
        assertThat(idx).isEqualTo(5)
    }

    @Test
    fun `finger between nine and twelve o'clock maps to slice 5 of 6`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(300f, 300f), 6, 50f)
        assertThat(idx).isEqualTo(5)
    }

    @Test
    fun `finger inside the hub dead zone hovers nothing`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(510f, 490f), 6, 50f)
        assertThat(idx).isNull()
    }

    @Test
    fun `finger exactly on the dead-zone radius hovers a slice`() {
        // The dead zone is a strict <: a distance equal to the radius already selects.
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(550f, 500f), 6, 50f)
        assertThat(idx).isEqualTo(1)
    }

    @Test
    fun `finger beyond the ring still selects by angle`() {
        // Game-wheel convention: there is no outer limit, only the hub dead zone.
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(500f, 5000f), 6, 50f)
        assertThat(idx).isEqualTo(3)
    }

    @Test
    fun `zero items hovers nothing`() {
        val idx = WheelMenuGeometry.hoveredIndex(center, Offset(700f, 500f), 0, 50f)
        assertThat(idx).isNull()
    }

    @Test
    fun `slice arcs start at twelve o'clock with the gap split evenly`() {
        // drawArc angles: 0 = 3 o'clock, so 12 o'clock = -90.
        assertThat(WheelMenuGeometry.sliceStartAngle(0, 6, 6f)).isEqualTo(-87f)
        assertThat(WheelMenuGeometry.sliceStartAngle(1, 6, 6f)).isEqualTo(-27f)
        assertThat(WheelMenuGeometry.sliceSweep(6, 6f)).isEqualTo(54f)
    }

    @Test
    fun `slice mid angle is the slice center`() {
        assertThat(WheelMenuGeometry.sliceMidAngle(0, 6)).isEqualTo(-60f)
        assertThat(WheelMenuGeometry.sliceMidAngle(3, 6)).isEqualTo(120f)
    }

    @Test
    fun `clampCenter pushes an edge press fully on screen`() {
        val clamped = WheelMenuGeometry.clampCenter(Offset(10f, 990f), 200f, 2000f, 1000f)
        assertThat(clamped).isEqualTo(Offset(200f, 800f))
    }

    @Test
    fun `clampCenter biases left and top when the surface is smaller than the wheel`() {
        // Degenerate surface: width - radius < radius. Must clamp, not throw.
        val clamped = WheelMenuGeometry.clampCenter(Offset(10f, 990f), 200f, 300f, 250f)
        assertThat(clamped).isEqualTo(Offset(200f, 200f))
    }

    @Test
    fun `clampCenter leaves an interior point alone`() {
        val clamped = WheelMenuGeometry.clampCenter(Offset(900f, 500f), 200f, 2000f, 1000f)
        assertThat(clamped).isEqualTo(Offset(900f, 500f))
    }
}
