package com.hakomi.practicetimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightScheme = lightColorScheme(
    primary = HakomiColors.ForestGreen,
    onPrimary = HakomiColors.Paper,
    primaryContainer = HakomiColors.Mist,
    onPrimaryContainer = HakomiColors.ForestGreenDeep,
    secondary = HakomiColors.Sage,
    onSecondary = HakomiColors.Paper,
    secondaryContainer = HakomiColors.MistDeep,
    onSecondaryContainer = HakomiColors.ForestGreenDeep,
    tertiary = HakomiColors.Slate,
    onTertiary = HakomiColors.Paper,
    tertiaryContainer = HakomiColors.SlateLight,
    onTertiaryContainer = HakomiColors.Ink,
    background = HakomiColors.Canvas,
    onBackground = HakomiColors.Ink,
    surface = HakomiColors.Paper,
    onSurface = HakomiColors.Ink,
    surfaceVariant = HakomiColors.Mist,
    onSurfaceVariant = HakomiColors.InkSoft,
    surfaceContainer = HakomiColors.Paper,
    surfaceContainerLow = HakomiColors.Canvas,
    surfaceContainerHigh = HakomiColors.Mist,
    surfaceContainerHighest = HakomiColors.MistDeep,
    outline = HakomiColors.Hairline,
    outlineVariant = HakomiColors.MistDeep,
    error = HakomiColors.Terracotta,
    onError = HakomiColors.Paper,
    errorContainer = HakomiColors.TerracottaSoft,
    onErrorContainer = HakomiColors.Terracotta,
    scrim = HakomiColors.ForestGreenDeep,
)

private val DarkScheme = darkColorScheme(
    primary = HakomiColors.SageLight,
    onPrimary = HakomiColors.ForestGreenDeep,
    primaryContainer = HakomiColors.NightMist,
    onPrimaryContainer = HakomiColors.SageLight,
    secondary = HakomiColors.Sage,
    onSecondary = HakomiColors.ForestGreenDeep,
    secondaryContainer = HakomiColors.NightMist,
    onSecondaryContainer = HakomiColors.NightInk,
    tertiary = HakomiColors.SlateLight,
    onTertiary = HakomiColors.NightCanvas,
    tertiaryContainer = HakomiColors.Slate,
    onTertiaryContainer = HakomiColors.NightInk,
    background = HakomiColors.NightCanvas,
    onBackground = HakomiColors.NightInk,
    surface = HakomiColors.NightPaper,
    onSurface = HakomiColors.NightInk,
    surfaceVariant = HakomiColors.NightMist,
    onSurfaceVariant = HakomiColors.NightInkSoft,
    surfaceContainer = HakomiColors.NightPaper,
    surfaceContainerLow = HakomiColors.NightCanvas,
    surfaceContainerHigh = HakomiColors.NightMist,
    surfaceContainerHighest = HakomiColors.NightHairline,
    outline = HakomiColors.NightHairline,
    outlineVariant = HakomiColors.NightMist,
    error = Color(0xFFD9917F),
    onError = HakomiColors.NightCanvas,
    errorContainer = Color(0xFF4A2E28),
    onErrorContainer = Color(0xFFF1D3CB),
)

val HakomiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun HakomiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = HakomiTypography,
        shapes = HakomiShapes,
        content = content,
    )
}
