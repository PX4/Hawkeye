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

    private var itemsState: List<WheelMenuItem> by mutableStateOf(initialItems)

    /**
     * Slice content; settable so hosts can swap menus (e.g. submenus) at runtime.
     * Swapping while the wheel is open clears [hoveredIndex] so a stale index can never
     * point past the new list; the next [move] re-resolves against the new slices, and a
     * finger that never moves again selects nothing on [release].
     */
    var items: List<WheelMenuItem>
        get() = itemsState
        set(value) {
            itemsState = value
            if (isOpen) hoveredIndex = null
        }

    /** True between [open] and the closing [release] or [cancel]; the widget draws only while open. */
    var isOpen: Boolean by mutableStateOf(false)
        private set

    /** Wheel center in px, clamped fully on-screen when [bounds] is known. */
    var center: Offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * Slice under the finger, resolved by [move] against the clamped [center]; null in
     * the hub dead zone, while closed, and before the first [move] of a gesture.
     */
    var hoveredIndex: Int? by mutableStateOf(null)
        private set

    /**
     * Synced from [WheelMenu]'s style as the wheel's full visual radius (ring plus
     * accent rim); used to clamp [center] so nothing draws off-screen.
     */
    var outerRadiusPx: Float = 0f

    /**
     * Synced from [WheelMenu]'s style (the hub radius); the cancel area. While unsynced
     * (<= 0) every [move] resolves to no hover, so keep [WheelMenu] in composition even
     * while the wheel is closed; it draws nothing then and is cheap.
     */
    var deadZoneRadiusPx: Float = 0f

    /** Synced from [WheelMenu]'s measured size; null until first layout. */
    var bounds: Size? = null

    /** Opens the wheel at [at] (clamped on-screen when [bounds] is known) with no hover. */
    fun open(at: Offset) {
        center = bounds?.let { WheelMenuGeometry.clampCenter(at, outerRadiusPx, it.width, it.height) } ?: at
        hoveredIndex = null
        isOpen = true
    }

    /**
     * Resolves [hoveredIndex] for the finger at [finger] against the clamped [center].
     * No-op while closed. While [deadZoneRadiusPx] is unsynced (<= 0), geometry is not
     * known yet, so everything counts as dead zone and nothing hovers.
     */
    fun move(finger: Offset) {
        if (!isOpen) return
        hoveredIndex = if (deadZoneRadiusPx <= 0f) {
            null
        } else {
            WheelMenuGeometry.hoveredIndex(center, finger, items.size, deadZoneRadiusPx)
        }
    }

    /**
     * Closes the wheel and returns the selected slice index. One-shot: returns null on
     * a second call, while closed, in the hub dead zone, or when the hovered index no
     * longer fits the current [items] list.
     */
    fun release(): Int? {
        val selected = if (isOpen) hoveredIndex?.takeIf { it < items.size } else null
        close()
        return selected
    }

    /** Closes the wheel without selecting; never reports a selection. */
    fun cancel() = close()

    private fun close() {
        isOpen = false
        hoveredIndex = null
    }
}

/**
 * Remembers a [WheelMenuState] seeded with [initialItems]. The list is read only at
 * first composition; swap menus later through [WheelMenuState.items]. Plain [remember]
 * (not rememberSaveable) is intentional: the wheel is gesture-transient, so there is no
 * open state worth restoring across configuration change or process death.
 */
@Composable
fun rememberWheelMenuState(initialItems: List<WheelMenuItem>): WheelMenuState =
    remember { WheelMenuState(initialItems) }
