package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.ui.geometry.Offset
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Pure slice math for [WheelMenu], kept UI-free so it unit-tests on the JVM and so any
 * gesture producer driving [WheelMenuState] resolves fingers to slices with the exact
 * angles the widget draws.
 *
 * Conventions: screen pixel space (y down); slice 0 starts at 12 o'clock and slices
 * proceed clockwise; arc angles follow Compose's drawArc (0 degrees = 3 o'clock).
 */
object WheelMenuGeometry {

    private const val FULL_CIRCLE = 360.0
    private const val TOP_OFFSET_DEGREES = 90.0

    /**
     * Index of the slice under [finger], or null inside the hub dead zone (the cancel
     * area) or when there are no items. There is deliberately no outer limit: like the
     * game wheels this follows, pointing past the ring still selects by angle.
     */
    fun hoveredIndex(
        center: Offset,
        finger: Offset,
        itemCount: Int,
        deadZoneRadius: Float,
    ): Int? {
        if (itemCount <= 0) return null
        val dx = finger.x - center.x
        val dy = finger.y - center.y
        if (hypot(dx, dy) < deadZoneRadius) return null
        val degrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        val fromTop = (degrees + TOP_OFFSET_DEGREES + FULL_CIRCLE) % FULL_CIRCLE
        return (fromTop / (FULL_CIRCLE / itemCount)).toInt().coerceAtMost(itemCount - 1)
    }

    /** drawArc start angle of slice [index], with the inter-slice gap split evenly. */
    fun sliceStartAngle(index: Int, itemCount: Int, gapDegrees: Float): Float {
        val span = FULL_CIRCLE.toFloat() / itemCount
        return -TOP_OFFSET_DEGREES.toFloat() + index * span + gapDegrees / 2f
    }

    /** drawArc sweep of one slice after removing the gap. */
    fun sliceSweep(itemCount: Int, gapDegrees: Float): Float =
        FULL_CIRCLE.toFloat() / itemCount - gapDegrees

    /** Angle of the middle of slice [index] (for placing the glyph and label). */
    fun sliceMidAngle(index: Int, itemCount: Int): Float {
        val span = FULL_CIRCLE.toFloat() / itemCount
        return -TOP_OFFSET_DEGREES.toFloat() + index * span + span / 2f
    }

    /** Clamp [center] so a wheel of [radius] stays fully inside a [width] x [height] surface. */
    fun clampCenter(center: Offset, radius: Float, width: Float, height: Float): Offset = Offset(
        x = center.x.coerceIn(radius, (width - radius).coerceAtLeast(radius)),
        y = center.y.coerceIn(radius, (height - radius).coerceAtLeast(radius)),
    )
}
