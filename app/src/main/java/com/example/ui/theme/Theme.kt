package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DeckLinkMonochromeColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = PureWhite,
    onPrimaryContainer = PureBlack,
    secondary = PureBlack,
    onSecondary = PureWhite,
    secondaryContainer = PureWhite,
    onSecondaryContainer = PureBlack,
    tertiary = PureBlack,
    onTertiary = PureWhite,
    tertiaryContainer = PureWhite,
    onTertiaryContainer = PureBlack,
    error = PureBlack,
    onError = PureWhite,
    errorContainer = PureWhite,
    onErrorContainer = PureBlack,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = PureWhite,
    onSurfaceVariant = PureBlack,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = PureWhite,
    surfaceContainer = PureWhite,
    surfaceContainerHigh = PureWhite,
    surfaceContainerHighest = PureWhite,
    outline = PureBlack,
    outlineVariant = PureBlack
)

val DeckLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DeckLinkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DeckLinkMonochromeColorScheme,
        typography = DeckLinkTypography,
        shapes = DeckLinkShapes,
        content = content
    )
}
