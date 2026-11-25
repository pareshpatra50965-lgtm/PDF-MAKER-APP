package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = PdfRed,
    onPrimary = Color.White,
    primaryContainer = PdfRedSoft,
    onPrimaryContainer = PdfRedDark,
    secondary = InkPrimary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = InkPrimary,
    tertiary = PdfRedBright,
    onTertiary = Color.White,
    tertiaryContainer = PdfRedTint,
    onTertiaryContainer = PdfRedDark,
    background = BackgroundWhite,
    onBackground = InkPrimary,
    surface = BackgroundWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceSubtle,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = BackgroundWhite,
    surfaceContainerLow = SurfaceSubtle,
    surfaceContainer = SurfaceSubtle,
    surfaceContainerHigh = SurfaceMuted,
    outline = OutlineHairline,
    outlineVariant = OutlineHairline,
    error = PdfRedDark,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PdfRedBright,
    onPrimary = Color.White,
    primaryContainer = PdfRedContainerDark,
    onPrimaryContainer = PdfRedLightOnDark,
    secondary = DarkInkPrimary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkInkPrimary,
    tertiary = PdfRedLightOnDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline
)

val PdfMakerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PdfMakerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PdfMakerTypography,
        shapes = PdfMakerShapes,
        content = content
    )
}
