package me.plexs.music.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun PlexTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PlexColors,
        typography = PlexTypography,
        content = content,
    )
}
