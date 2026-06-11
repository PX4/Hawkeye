package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Gesture-source-agnostic driver for [WheelMenu].
 *
 * The widget is render-only; this class is the seam that makes it reusable in any app:
 * a producer — [Modifier.wheelMenuGestures][wheelMenuGestures] in a pure-Compose host, a
 * poll loop over a native gesture bridge, or a test — calls [open], [move], [release],
 * and [cancel], and the widget projects the resulting state. All coordinates are px in
 * the coordinate space of the composable hosting [WheelMenu].
 *
 * Hit-test inputs ([outerRadiusPx], [deadZoneRadiusPx], [bounds]) are synced by
 * [WheelMenu] from its style and measured size, so the producer's selection math and the
 * widget's drawing always agree. Tests set them directly and never need a UI.
 */
@Stable
class WheelMenuState(initialItems: List<WheelMenuItem>) {

    /** Slice content; settable so hosts can swap menus (e.g. submenus) at runtime. */
    var items: List<WheelMenuItem> by mutableStateOf(initialItems)

    var isOpen: Boolean by mutableStateOf(false)
        private set

    /** Wheel center in px, clamped fully on-screen when [bounds] is known. */
    var center: Offset by mutableStateOf(Offset.Zero)
        private set

    var hoveredIndex: Int? by mutableStateOf(null)
        private set

    /** Synced from [WheelMenu]'s style; used to clamp [center]. */
    var outerRadiusPx: Float = 0f

    /** Synced from [WheelMenu]'s style (the hub radius); the cancel area. */
    var deadZoneRadiusPx: Float = 0f

    /** Synced from [WheelMenu]'s measured size; null until first layout. */
    var bounds: Size? = null

    fun open(at: Offset) {
        center = bounds?.let { WheelMenuGeometry.clampCenter(at, outerRadiusPx, it.width, it.height) } ?: at
        hoveredIndex = null
        isOpen = true
    }

    fun move(finger: Offset) {
        if (!isOpen) return
        hoveredIndex = WheelMenuGeometry.hoveredIndex(center, finger, items.size, deadZoneRadiusPx)
    }

    fun release(): Int? {
        val selected = if (isOpen) hoveredIndex else null
        close()
        return selected
    }

    fun cancel() = close()

    private fun close() {
        isOpen = false
        hoveredIndex = null
    }
}

/** Remembers a [WheelMenuState] for [items] across recompositions. */
@Composable
fun rememberWheelMenuState(items: List<WheelMenuItem>): WheelMenuState =
    remember { WheelMenuState(items) }
