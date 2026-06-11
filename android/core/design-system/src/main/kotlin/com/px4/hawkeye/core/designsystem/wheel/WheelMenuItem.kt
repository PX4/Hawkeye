package com.px4.hawkeye.core.designsystem.wheel

import androidx.compose.ui.graphics.Color

/** One wheel slice: a label plus the accent color of its glyph dot. */
data class WheelMenuItem(
    val label: String,
    val accentColor: Color,
)
