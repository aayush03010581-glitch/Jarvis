package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisDarkColorScheme = darkColorScheme(
    primary = HoloCyan,
    onPrimary = HoloBgDark,
    primaryContainer = HoloSurfaceVariant,
    onPrimaryContainer = HoloCyanBright,
    secondary = StarkGold,
    onSecondary = HoloBgDark,
    secondaryContainer = HoloSurfaceDark,
    onSecondaryContainer = StarkGoldBright,
    tertiary = AlertRed,
    onTertiary = Color.White,
    background = HoloBgDark,
    onBackground = HoloTextPrimary,
    surface = HoloSurfaceDark,
    onSurface = HoloTextPrimary,
    surfaceVariant = HoloSurfaceVariant,
    onSurfaceVariant = HoloTextSecondary,
    outline = HoloCardBorder,
    outlineVariant = HoloCardBorderGlow,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}

