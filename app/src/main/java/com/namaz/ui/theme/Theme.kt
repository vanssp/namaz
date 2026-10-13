package com.namaz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Ivory,
    secondary = Brass,
    onSecondary = Ivory,
    background = Paper,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkMuted,
    outline = Line,
)

private val DarkColors = darkColorScheme(
    primary = NightBrass,
    onPrimary = Night,
    secondary = NightBrass,
    onSecondary = Night,
    background = Night,
    onBackground = NightInk,
    surface = NightRaised,
    onSurface = NightInk,
    surfaceVariant = NightRaised,
    onSurfaceVariant = NightMuted,
    outline = NightLine,
)

@Composable
fun NamazTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = NamazTypography,
        content = content,
    )
}
