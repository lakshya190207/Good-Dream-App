package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = DarkForestGreenPrimary,       // #52B788: Luminous Emerald/Sage for dark surfaces
    onPrimary = Color(0xFF042114),
    primaryContainer = DarkForestGreenContainer, // #1B382B: Deep Forest Pine Container
    onPrimaryContainer = Color(0xFFD8F3DC),
    secondary = DarkSatinGoldAccent,        // #F0D278: Luminous Satin Gold with strong contrast
    onSecondary = Color(0xFF2C2200),
    secondaryContainer = DarkSatinGoldContainer, // #352B11: Warm Night Gold Container
    onSecondaryContainer = DarkSatinGoldLight,
    tertiary = DarkForestGreenLight,
    onTertiary = Color(0xFF042114),
    tertiaryContainer = DarkForestGreenContainer,
    onTertiaryContainer = Color(0xFFD8F3DC),
    background = DarkNightBackground,       // #0C1612: Deep Pine Night Sanctuary
    surface = DarkNightSurface,             // #16251E: Elevated Night Forest Card
    onBackground = DarkTextPrimary,         // #F1F5F2: High-contrast soft ivory text
    onSurface = DarkTextPrimary,            // #F1F5F2: High-contrast soft ivory text
    surfaceVariant = DarkNightSurfaceSubtle, // #1F3329: Subtle Forest Surface
    onSurfaceVariant = DarkTextSecondary,   // #A6B8AE: Legible soothing sage slate
    outline = DarkBorderSubtle,             // #2C4438: Subtle dark contour
    outlineVariant = Color(0xFF23372E),
    error = StatusErrorLight,
    onError = Color(0xFF690005),
    errorContainer = StatusErrorContainerDark,
    onErrorContainer = StatusOnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = DeepElegantGreen,             // #1B4332: Deep Elegant Forest Green
    onPrimary = Color.White,
    primaryContainer = ForestGreenContainer,// #E8F3EE: Soft Forest Container
    onPrimaryContainer = ForestGreenDark,
    secondary = SatinGoldAccent,            // #D4AF37: Classic Satin Gold Accent
    onSecondary = TextPrimaryDark,
    secondaryContainer = SatinGoldContainer,// #FBF6EA: Warm Gold Container
    onSecondaryContainer = SatinGoldDark,
    tertiary = SatinGoldAccent,
    onTertiary = TextPrimaryDark,
    tertiaryContainer = SatinGoldLight,
    onTertiaryContainer = ForestGreenDark,
    background = SoftCreamBackground,       // #FDFBF7: Soft Cream Sanctuary
    surface = CardSurfaceWhite,             // #FFFFFF: Clean Card Surface
    onBackground = TextPrimaryDark,         // #19241C: Deep Charcoal Green Text
    onSurface = TextPrimaryDark,            // #19241C
    surfaceVariant = SurfaceSubtle,         // #F7F5F0: Warm Muted Grey-Cream
    onSurfaceVariant = TextSecondaryMuted,  // #5D6B63: Muted Slate Text
    outline = BorderSubtle,                 // #E5E0D8: Subtle Border
    outlineVariant = Color(0xFFD9D4CC),
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorContainerLight,
    onErrorContainer = StatusOnErrorContainerLight
)

@Composable
fun GoodDreamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GoodDreamTypography,
            shapes = GoodDreamShapes,
            content = content
        )
    }
}

/** Backwards-compatible alias for existing callers */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GoodDreamTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
