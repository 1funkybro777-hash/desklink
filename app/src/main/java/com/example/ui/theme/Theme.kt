package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeckLinkSmartColorScheme = lightColorScheme(
    primary = SmartBlue,
    onPrimary = PureWhite,
    primaryContainer = SmartBlueTint,
    onPrimaryContainer = SmartSlateTitle,
    secondary = SmartSlateTitle,
    onSecondary = PureWhite,
    secondaryContainer = SmartSearchPill,
    onSecondaryContainer = SmartInk,
    tertiary = SmartLiveGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SmartLiveGreenTint,
    onTertiaryContainer = SmartLiveGreen,
    error = SmartDanger,
    onError = PureWhite,
    background = SmartCanvas,
    onBackground = SmartInk,
    surface = SmartCanvas,
    onSurface = SmartInk,
    surfaceVariant = SmartSurface,
    onSurfaceVariant = SmartInkMuted,
    surfaceContainerLowest = SmartSurfaceElevated,
    surfaceContainerLow = SmartSurface,
    surfaceContainer = SmartCanvas,
    surfaceContainerHigh = SmartSearchPill,
    surfaceContainerHighest = SmartPlusCircle,
    outline = SmartBorder,
    outlineVariant = SmartBorder
)

val DeckLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DeckLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeckLinkSmartColorScheme,
        typography = DeckLinkTypography,
        shapes = DeckLinkShapes,
        content = content
    )
}
