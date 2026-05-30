package com.px4.hawkeye.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dronecode Foundation brand palette (dronecode.org): a green primary with a cyan
// companion (the two appear together in their cyan-to-green gradients), over dark slate
// surfaces. Exposed for places that render over media where the theme's light/dark
// primary would not stay legible (e.g. the Home title over the background video).
val DronecodeGreen = Color(0xFF1CB571)
val DronecodeCyan = Color(0xFF2AC4EA)

// Translucent "glass" surface for chrome layered over media (the Home nav bar and cards).
// Theme-aware: derived from surfaceVariant, so it is a light glass in the light scheme and a
// dark glass in the dark scheme. The alpha is baked in here so call sites share one token
// instead of scattering copy(alpha=...). 0.75f is a touch more opaque than a plain card.
val ColorScheme.glassSurface: Color
    get() = surfaceVariant.copy(alpha = HawkeyeAlpha.GLASS)

private val DarkColors = darkColorScheme(
    primary = DronecodeGreen,
    onPrimary = Color(0xFF04140C),
    secondary = DronecodeCyan,
    onSecondary = Color(0xFF03242E),
    background = Color(0xFF0B0E13),
    surface = Color(0xFF141A22),
    surfaceVariant = Color(0xFF1C242F),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7A4B),
    onPrimary = Color.White,
    secondary = Color(0xFF0F7A86),
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
