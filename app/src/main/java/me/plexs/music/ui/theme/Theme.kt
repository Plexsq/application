package me.plexs.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkAccent = Color(0xFFA855F7)
private val DarkOnAccent = Color(0xFF1A0A2E)

private val PlexColors = darkColorScheme(
    primary = PlexAccent,
    onPrimary = PlexOnAccent,
    primaryContainer = PlexAccent,
    onPrimaryContainer = PlexOnAccent,
    background = PlexBackground,
    onBackground = PlexText,
    surface = PlexSurface,
    onSurface = PlexText,
    surfaceVariant = PlexSurfaceVariant,
    onSurfaceVariant = PlexMuted,
    outline = PlexOutline,
    error = PlexError,
)

private val PlexLightColors = lightColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,
    primaryContainer = DarkAccent,
    onPrimaryContainer = Color.White,
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF1C1B1B),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1B),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF6B6B6B),
    outline = Color(0xFFD0D0D0),
    error = Color(0xFFB3261E),
)

/** True when the active scheme is dark; drives screen-level color choices. */
val LocalIsDark = staticCompositionLocalOf { true }

fun accentColor(accentHex: String): Color {
    val c = accentHex.removePrefix("#")
    if (c.length != 6) return DarkAccent
    return runCatching { Color(0xFF000000 or c.toLong(16)) }.getOrDefault(DarkAccent)
}

@Composable
fun PlexTheme(
    dark: Boolean = true,
    accent: Color = PlexAccent,
    content: @Composable () -> Unit,
) {
    val base = if (dark) PlexColors else PlexLightColors
    val scheme = base.copy(primary = accent, primaryContainer = accent, onPrimary = if (dark) PlexOnAccent else Color.White, onPrimaryContainer = if (dark) PlexOnAccent else Color.White)
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PlexTypography,
            content = content,
        )
    }
}