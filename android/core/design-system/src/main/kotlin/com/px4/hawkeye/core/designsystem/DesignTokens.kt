package com.px4.hawkeye.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp

/**
 * Centralized design tokens so spacing, alphas, scrim, and shadows are defined once and
 * reused across screens instead of being hard-coded inline at each call site.
 */
object HawkeyeDimens {
    /** Outer padding for a primary/hero screen (Home). */
    val screenPadding = 24.dp

    /** Outer padding for standard content screens (Settings). */
    val contentPadding = 16.dp

    /** Gap between major sections. */
    val sectionSpacing = 32.dp

    /** Gap between sibling items (e.g. cards, section headers). */
    val itemSpacing = 16.dp

    /** Inner padding of a card. */
    val cardPadding = 20.dp

    /** Resting elevation of a card. */
    val cardElevation = 2.dp

    /** Gap under a title/header. */
    val titleSpacing = 8.dp

    /** Gap between a card title and its caption/description. */
    val captionSpacing = 6.dp

    /** Vertical padding for a selectable row. */
    val rowVerticalPadding = 4.dp

    /** Small inline gap (e.g. between a control and its label). */
    val inlineSpacing = 8.dp
}

/** Opacity tokens for translucent surfaces and text rendered over media. */
object HawkeyeAlpha {
    /** Translucent "glass" chrome over media. */
    const val GLASS = 0.75f

    /** Dark scrim over the background video for legibility. */
    const val SCRIM = 0.45f

    /** Secondary text rendered over the video (e.g. the Home subtitle). */
    const val ON_MEDIA_SECONDARY = 0.85f

    /** Card caption/description text. */
    const val CARD_CAPTION = 0.65f
}

/** Dark scrim drawn over the background video so foreground content stays legible. */
val ScrimColor = Color.Black.copy(alpha = HawkeyeAlpha.SCRIM)

/** Subtle drop shadow for text rendered over media (e.g. the Home title). */
val MediaTitleShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 2f),
    blurRadius = 6f,
)
