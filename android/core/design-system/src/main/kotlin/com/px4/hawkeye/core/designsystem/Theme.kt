package com.px4.hawkeye.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// HUD-inspired palette (see design mockups): cyan primary, amber accent, dark slate surfaces.
private val Cyan = Color(0xFF5CD0E6)
private val OnCyan = Color(0xFF06222A)
private val Amber = Color(0xFFFFB454)
private val OnAmber = Color(0xFF241A06)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = OnCyan,
    secondary = Amber,
    onSecondary = OnAmber,
    background = Color(0xFF0B0E13),
    surface = Color(0xFF141A22),
    surfaceVariant = Color(0xFF1C242F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF00687A),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5A00),
    onSecondary = Color.White,
)

// Material 3 Expressive theming (MaterialExpressiveTheme / MotionScheme) is internal in
// material3 1.4.0 stable, so we use the standard MaterialTheme with the HUD-dark palette.
// Switch to MaterialExpressiveTheme once it is public in a stable material3 release.
@Composable
fun HawkeyeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
