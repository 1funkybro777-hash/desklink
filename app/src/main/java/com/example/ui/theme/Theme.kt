package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeckLinkLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = CrispWhiteCard,
    primaryContainer = PeriwinkleHero,
    onPrimaryContainer = DeepNavyInk,
    secondary = LiveGreen,
    onSecondary = CrispWhiteCard,
    secondaryContainer = LiveGreenSoft,
    onSecondaryContainer = LiveGreen,
    tertiary = WarmAmber,
    onTertiary = CrispWhiteCard,
    tertiaryContainer = WarmAmberSoft,
    onTertiaryContainer = WarmAmber,
    error = SoftCrimson,
    onError = CrispWhiteCard,
    errorContainer = SoftCrimsonSoft,
    onErrorContainer = SoftCrimson,
    background = PorcelainCanvas,
    onBackground = InkCharcoal,
    surface = CrispWhiteCard,
    onSurface = InkCharcoal,
    surfaceVariant = WarmStoneTile,
    onSurfaceVariant = InkSlate,
    surfaceContainerLowest = CrispWhiteCard,
    surfaceContainerLow = PorcelainCanvas,
    surfaceContainer = WarmStoneTile,
    surfaceContainerHigh = WarmStonePressed,
    surfaceContainerHighest = WarmStonePressed,
    outline = WarmHairline,
    outlineVariant = WarmSubtleHairline
)

val DeckLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeckLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeckLinkLightColorScheme,
        typography = DeckLinkTypography,
        shapes = DeckLinkShapes,
        content = content
    )
}
