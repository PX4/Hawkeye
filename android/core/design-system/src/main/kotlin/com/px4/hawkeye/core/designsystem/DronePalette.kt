package com.px4.hawkeye.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Per-drone accent colors for multi-drone (swarm) sessions, mirroring the native renderer's
 * default theme `drone_palette` in `src/theme.c` so Compose chrome (wheel menu slices, labels)
 * matches the drone meshes and trails drawn by the engine. Index = drone index; consumers wrap
 * with `[i % colors.size]` exactly like the C side. Update both places together if the native
 * default palette ever changes.
 */
object HawkeyeDronePalette {
    val colors: List<Color> = listOf(
        Color(230, 230, 230), //  0: white (primary)
        Color(40, 120, 255), //  1: blue
        Color(255, 40, 80), //  2: red
        Color(255, 200, 40), //  3: yellow
        Color(40, 220, 80), //  4: green
        Color(255, 140, 0), //  5: orange
        Color(180, 60, 255), //  6: purple
        Color(255, 100, 160), //  7: pink
        Color(0, 200, 200), //  8: teal
        Color(255, 220, 100), //  9: gold
        Color(100, 100, 255), // 10: indigo
        Color(255, 180, 140), // 11: peach
        Color(140, 255, 200), // 12: mint
        Color(255, 60, 200), // 13: magenta
        Color(140, 200, 255), // 14: sky blue
        Color(200, 255, 60), // 15: lime
    )
}
