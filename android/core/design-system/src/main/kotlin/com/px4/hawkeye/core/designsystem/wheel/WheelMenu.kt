package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import kotlin.math.cos
import kotlin.math.sin

object WheelMenuTestTags {
    const val WHEEL = "wheel_menu"
}

/**
 * The HUD Ring radial menu: a glass annulus of gap-separated arc slices around a center
 * hub that echoes the hovered item and shows [selectHint] ("release to select").
 *
 * Render-only by design: it has no gesture handling and never consumes touch input. A
 * producer drives [state] — use [Modifier.wheelMenuGestures][wheelMenuGestures] in a
 * pure-Compose host, or call the [WheelMenuState] methods from any other gesture source.
 * The composable fills its parent and draws nothing while the wheel is closed; it also
 * syncs its measured size and hit radii into [state] so producer-side selection math and
 * the drawing always agree.
 */
@Composable
fun WheelMenu(
    state: WheelMenuState,
    selectHint: String,
    modifier: Modifier = Modifier,
    style: WheelMenuStyle = WheelMenuDefaults.style(),
) {
    val density = LocalDensity.current
    SideEffect {
        state.outerRadiusPx = with(density) { style.outerRadius.toPx() }
        state.deadZoneRadiusPx = with(density) { style.hubRadius.toPx() }
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelLarge
    val hubStyle = MaterialTheme.typography.titleMedium
    val hintStyle = MaterialTheme.typography.labelSmall

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { state.bounds = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        if (!state.isOpen || state.items.isEmpty()) return@Box
        val items = state.items
        val center = state.center
        val hoveredIndex = state.hoveredIndex

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag(WheelMenuTestTags.WHEEL),
        ) {
            val outerR = style.outerRadius.toPx()
            val thickness = style.ringThickness.toPx()
            val midR = outerR - thickness / 2f
            val hubR = style.hubRadius.toPx()
            val edgeW = style.edgeWidth.toPx()
            val arcTopLeft = Offset(center.x - midR, center.y - midR)
            val arcSize = Size(midR * 2f, midR * 2f)

            // Glass annulus under the slices (slightly wider so it reads as the ring's body).
            drawCircle(
                color = style.ringColor,
                radius = midR,
                center = center,
                style = Stroke(width = thickness + style.edgeGap.toPx() * 2f),
            )

            items.forEachIndexed { index, item ->
                val hovered = index == hoveredIndex
                drawArc(
                    color = if (hovered) style.hoveredSliceColor else style.sliceColor,
                    startAngle = WheelMenuGeometry.sliceStartAngle(index, items.size, style.gapDegrees),
                    sweepAngle = WheelMenuGeometry.sliceSweep(items.size, style.gapDegrees),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = thickness),
                )

                // Thin accent arc on the outer rim, brighter on the hovered slice.
                val edgeR = outerR + style.edgeGap.toPx()
                drawArc(
                    color = if (hovered) style.hoveredBorderColor else style.borderColor,
                    startAngle = WheelMenuGeometry.sliceStartAngle(index, items.size, style.gapDegrees),
                    sweepAngle = WheelMenuGeometry.sliceSweep(items.size, style.gapDegrees),
                    useCenter = false,
                    topLeft = Offset(center.x - edgeR, center.y - edgeR),
                    size = Size(edgeR * 2f, edgeR * 2f),
                    style = Stroke(width = edgeW),
                )

                // Glyph dot above the label, both at the slice midpoint radius.
                val midRad = Math.toRadians(
                    WheelMenuGeometry.sliceMidAngle(index, items.size).toDouble(),
                )
                val gx = center.x + midR * cos(midRad).toFloat()
                val gy = center.y + midR * sin(midRad).toFloat()
                drawCircle(
                    color = item.accentColor,
                    radius = style.glyphRadius.toPx(),
                    center = Offset(gx, gy - style.glyphLift.toPx()),
                )
                val label = textMeasurer.measure(item.label, labelStyle)
                drawText(
                    textLayoutResult = label,
                    color = if (hovered) style.hoveredLabelColor else style.labelColor,
                    topLeft = Offset(
                        gx - label.size.width / 2f,
                        gy + style.labelDrop.toPx() - label.size.height / 2f,
                    ),
                )
            }

            // Hub: hovered label echo + the release hint.
            drawCircle(color = style.hubColor, radius = hubR, center = center)
            drawCircle(
                color = style.borderColor,
                radius = hubR,
                center = center,
                style = Stroke(width = edgeW),
            )
            val hubLabel = hoveredIndex?.let { items.getOrNull(it)?.label }
            if (hubLabel != null) {
                val hub = textMeasurer.measure(hubLabel, hubStyle)
                drawText(
                    textLayoutResult = hub,
                    color = style.hoveredLabelColor,
                    topLeft = Offset(
                        center.x - hub.size.width / 2f,
                        center.y - style.hintGap.toPx() - hub.size.height,
                    ),
                )
            }
            val hint = textMeasurer.measure(selectHint, hintStyle)
            drawText(
                textLayoutResult = hint,
                color = style.hintColor,
                topLeft = Offset(
                    center.x - hint.size.width / 2f,
                    center.y + style.hintGap.toPx(),
                ),
            )
        }
    }
}

@Preview(widthDp = 480, heightDp = 480)
@Composable
private fun WheelMenuPreview() {
    HawkeyeTheme(darkTheme = true) {
        val state = remember {
            WheelMenuState(
                listOf(
                    WheelMenuItem("Drone 1", Color(0xFFE6E6E6)),
                    WheelMenuItem("Drone 2", Color(0xFF2878FF)),
                    WheelMenuItem("Drone 3", Color(0xFFFF2850)),
                    WheelMenuItem("Drone 4", Color(0xFFFFC828)),
                    WheelMenuItem("Drone 5", Color(0xFF28DC50)),
                    WheelMenuItem("Drone 6", Color(0xFFFF8C00)),
                ),
            ).apply {
                deadZoneRadiusPx = 170f
                open(Offset(640f, 640f))
                move(Offset(1000f, 640f)) // hover slice 1
            }
        }
        Box(
            modifier = Modifier
                .size(480.dp)
                .background(Color(0xFF202022)),
        ) {
            WheelMenu(state = state, selectHint = "Release to select")
        }
    }
}
