package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeckLinkDarkColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = OnSignalCyan,
    primaryContainer = SignalCyanContainer,
    onPrimaryContainer = SignalCyan,
    secondary = LiveEmerald,
    onSecondary = OnLiveEmerald,
    secondaryContainer = LiveEmeraldContainer,
    onSecondaryContainer = LiveEmerald,
    tertiary = SignalAmber,
    onTertiary = OnSignalAmber,
    tertiaryContainer = SignalAmberContainer,
    onTertiaryContainer = SignalAmber,
    error = AlertCrimson,
    onError = OnAlertCrimson,
    errorContainer = AlertCrimsonContainer,
    onErrorContainer = OnAlertCrimson,
    background = ObsidianBase,
    onBackground = InkPrimary,
    surface = CarbonSurface,
    onSurface = InkPrimary,
    surfaceVariant = SlateSurfaceElevated,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = ObsidianBase,
    surfaceContainerLow = CarbonSurface,
    surfaceContainer = SlateSurfaceElevated,
    surfaceContainerHigh = SlateSurfaceHigh,
    surfaceContainerHighest = SlateSurfaceHigh,
    outline = HairlineOutline,
    outlineVariant = HairlineSubtle
)

val DeckLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeckLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeckLinkDarkColorScheme,
        typography = DeckLinkTypography,
        shapes = DeckLinkShapes,
        content = content
    )
}
