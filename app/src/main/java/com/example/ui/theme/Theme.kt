package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = CoralSecondary,
    onSecondary = CoralOnSecondary,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = CoralOnContainer,
    tertiary = CobaltTertiary,
    onTertiary = CobaltOnTertiary,
    tertiaryContainer = CobaltContainer,
    onTertiaryContainer = CobaltOnContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = CoralSecondaryDark,
    onSecondary = CoralOnSecondaryDark,
    secondaryContainer = CoralContainerDark,
    onSecondaryContainer = CoralOnContainerDark,
    tertiary = CobaltTertiaryDark,
    onTertiary = CobaltOnTertiaryDark,
    tertiaryContainer = CobaltContainerDark,
    onTertiaryContainer = CobaltOnContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun FitCalendarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val fitnessColors = if (darkTheme) DarkFitnessColors else LightFitnessColors

    CompositionLocalProvider(LocalFitnessColors provides fitnessColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FitTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
