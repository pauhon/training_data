package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Editorial Athletic Light Palette
val EmeraldPrimary = Color(0xFF0E8A5F)
val EmeraldOnPrimary = Color(0xFFFFFFFF)
val EmeraldContainer = Color(0xFFD8F3E7)
val EmeraldOnContainer = Color(0xFF063B27)

val CoralSecondary = Color(0xFFE85D2A)
val CoralOnSecondary = Color(0xFFFFFFFF)
val CoralContainer = Color(0xFFFDE6DC)
val CoralOnContainer = Color(0xFF5A1E08)

val CobaltTertiary = Color(0xFF3563E9)
val CobaltOnTertiary = Color(0xFFFFFFFF)
val CobaltContainer = Color(0xFFE1EAFF)
val CobaltOnContainer = Color(0xFF102A70)

val LightBackground = Color(0xFFF5F6F2)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFECEFE9)
val LightSurfaceContainerLow = Color(0xFFFAFBF8)
val LightSurfaceContainer = Color(0xFFF0F2ED)
val LightSurfaceContainerHigh = Color(0xFFE7EAE3)
val LightOnSurface = Color(0xFF151A17)
val LightOnSurfaceVariant = Color(0xFF55605A)
val LightOutline = Color(0xFFD5DCD6)
val LightOutlineVariant = Color(0xFFE6EBE6)

// Editorial Athletic Dark Palette
val EmeraldPrimaryDark = Color(0xFF4CD9A4)
val EmeraldOnPrimaryDark = Color(0xFF003824)
val EmeraldContainerDark = Color(0xFF0B5238)
val EmeraldOnContainerDark = Color(0xFFC2F5DF)

val CoralSecondaryDark = Color(0xFFFF8F66)
val CoralOnSecondaryDark = Color(0xFF4A1804)
val CoralContainerDark = Color(0xFF6B280D)
val CoralOnContainerDark = Color(0xFFFFDBD0)

val CobaltTertiaryDark = Color(0xFF8AA8FF)
val CobaltOnTertiaryDark = Color(0xFF092366)
val CobaltContainerDark = Color(0xFF1C3A8C)
val CobaltOnContainerDark = Color(0xFFDCE5FF)

val DarkBackground = Color(0xFF0E1311)
val DarkSurface = Color(0xFF151C19)
val DarkSurfaceVariant = Color(0xFF1E2823)
val DarkSurfaceContainerLow = Color(0xFF131916)
val DarkSurfaceContainer = Color(0xFF19221E)
val DarkSurfaceContainerHigh = Color(0xFF222D28)
val DarkOnSurface = Color(0xFFECF2EE)
val DarkOnSurfaceVariant = Color(0xFFA2B0A8)
val DarkOutline = Color(0xFF36443D)
val DarkOutlineVariant = Color(0xFF26312C)

val ErrorLight = Color(0xFFC93838)
val ErrorContainerLight = Color(0xFFFCE4E4)
val ErrorDark = Color(0xFFFF8A8A)
val ErrorContainerDark = Color(0xFF5E1B1B)

@Immutable
data class FitnessSemanticColors(
    val strength: Color,
    val strengthSoft: Color,
    val run: Color,
    val runSoft: Color,
    val weight: Color,
    val weightSoft: Color,
    val upperAccent: Color,
    val lowerAccent: Color,
    val crunchAccent: Color,
    val situpAccent: Color,
    val positiveTrend: Color,
    val warningAccent: Color,
    val cardElevated: Color,
)

val LightFitnessColors = FitnessSemanticColors(
    strength = EmeraldPrimary,
    strengthSoft = Color(0xFFE3F5EC),
    run = CoralSecondary,
    runSoft = Color(0xFFFDECE5),
    weight = CobaltTertiary,
    weightSoft = Color(0xFFE7EEFF),
    upperAccent = Color(0xFF0E8A5F),
    lowerAccent = Color(0xFF1E9E89),
    crunchAccent = Color(0xFFD98218),
    situpAccent = Color(0xFF7A5AF8),
    positiveTrend = Color(0xFF0E8A5F),
    warningAccent = Color(0xFFD98218),
    cardElevated = Color(0xFFFFFFFF),
)

val DarkFitnessColors = FitnessSemanticColors(
    strength = EmeraldPrimaryDark,
    strengthSoft = Color(0xFF123628),
    run = CoralSecondaryDark,
    runSoft = Color(0xFF3E1F14),
    weight = CobaltTertiaryDark,
    weightSoft = Color(0xFF172752),
    upperAccent = Color(0xFF4CD9A4),
    lowerAccent = Color(0xFF4FD1C5),
    crunchAccent = Color(0xFFF6AD55),
    situpAccent = Color(0xFFB794F4),
    positiveTrend = Color(0xFF4CD9A4),
    warningAccent = Color(0xFFF6AD55),
    cardElevated = Color(0xFF19221E),
)

val LocalFitnessColors = staticCompositionLocalOf { LightFitnessColors }
