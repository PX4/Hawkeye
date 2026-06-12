package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.px4.hawkeye.core.designsystem.glassSurface

/**
 * One wheel slice: a label plus the accent color of its glyph dot.
 *
 * @property label Slice text, drawn on a single line; keep it short (a word or two).
 * @property accentColor Color of the glyph dot above the label.
 * @property hubLabel Optional detail text the hub echoes while this slice is hovered
 *   (e.g. a file name when the slice label is just an index); falls back to [label].
 *   Drawn on a single line — the producer should pre-truncate anything that could
 *   outgrow the hub.
 */
data class WheelMenuItem(
    val label: String,
    val accentColor: Color,
    val hubLabel: String? = null,
)

/**
 * Every visual knob of [WheelMenu]. Dimensions are Dp so the wheel scales with density;
 * colors default to the active Material scheme via [WheelMenuDefaults.style] but can be
 * overridden wholesale by reusers.
 */
data class WheelMenuStyle(
    val outerRadius: Dp,
    val ringThickness: Dp,
    val hubRadius: Dp,
    val gapDegrees: Float,
    val edgeWidth: Dp,
    val edgeGap: Dp,
    val glyphRadius: Dp,
    val glyphLift: Dp,
    val labelDrop: Dp,
    val hintGap: Dp,
    val ringColor: Color,
    val sliceColor: Color,
    val hoveredSliceColor: Color,
    val borderColor: Color,
    val hoveredBorderColor: Color,
    val labelColor: Color,
    val hoveredLabelColor: Color,
    val hubColor: Color,
    val hintColor: Color,
)

object WheelMenuDefaults {

    val OuterRadius = 168.dp
    val HubRadius = 64.dp

    private val RingThickness = 64.dp
    private const val GAP_DEGREES = 6f
    private val EdgeWidth = 2.dp
    private val EdgeGap = 3.dp
    private val GlyphRadius = 5.dp
    private val GlyphLift = 12.dp
    private val LabelDrop = 12.dp
    private val HintGap = 6.dp

    private const val SLICE_ALPHA = 0.10f
    private const val HOVERED_SLICE_ALPHA = 0.55f
    private const val BORDER_ALPHA = 0.39f
    private const val HUB_ALPHA = 0.94f
    private const val HINT_ALPHA = 0.55f

    @Composable
    fun style(): WheelMenuStyle {
        val scheme = MaterialTheme.colorScheme
        return WheelMenuStyle(
            outerRadius = OuterRadius,
            ringThickness = RingThickness,
            hubRadius = HubRadius,
            gapDegrees = GAP_DEGREES,
            edgeWidth = EdgeWidth,
            edgeGap = EdgeGap,
            glyphRadius = GlyphRadius,
            glyphLift = GlyphLift,
            labelDrop = LabelDrop,
            hintGap = HintGap,
            ringColor = scheme.glassSurface,
            sliceColor = scheme.onSurface.copy(alpha = SLICE_ALPHA),
            hoveredSliceColor = scheme.primary.copy(alpha = HOVERED_SLICE_ALPHA),
            borderColor = scheme.primary.copy(alpha = BORDER_ALPHA),
            hoveredBorderColor = scheme.primary,
            labelColor = scheme.onSurface,
            hoveredLabelColor = scheme.onSurface,
            hubColor = scheme.surface.copy(alpha = HUB_ALPHA),
            hintColor = scheme.onSurface.copy(alpha = HINT_ALPHA),
        )
    }
}
