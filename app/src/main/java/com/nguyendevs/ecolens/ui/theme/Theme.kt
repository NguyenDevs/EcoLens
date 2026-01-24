package com.nguyendevs.ecolens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Light color scheme for EcoLens. */
private val LightColorScheme =
        lightColorScheme(
                primary = Primary,
                onPrimary = OnPrimary,
                primaryContainer = SurfaceTint,
                onPrimaryContainer = PrimaryDark,
                secondary = PrimaryLight,
                onSecondary = OnPrimary,
                secondaryContainer = SuccessBg,
                onSecondaryContainer = SuccessText,
                tertiary = Info,
                onTertiary = Color.White,
                tertiaryContainer = InfoBg,
                onTertiaryContainer = InfoText,
                error = Error,
                onError = Color.White,
                errorContainer = ErrorBg,
                onErrorContainer = ErrorText,
                background = Background,
                onBackground = OnSurface,
                surface = Surface,
                onSurface = OnSurface,
                surfaceVariant = SurfaceVariant,
                onSurfaceVariant = TextSecondary,
                outline = BorderNormal,
                outlineVariant = BorderLight,
                scrim = Scrim
        )

/** Dark color scheme for EcoLens. */
private val DarkColorScheme =
        darkColorScheme(
                primary = PrimaryDarkTheme,
                onPrimary = OnPrimary,
                primaryContainer = PrimaryDark,
                onPrimaryContainer = PrimaryLight,
                secondary = PrimaryLight,
                onSecondary = OnPrimary,
                secondaryContainer = Color(0xFF1B3B2F),
                onSecondaryContainer = SuccessBg,
                tertiary = Info,
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFF0D47A1),
                onTertiaryContainer = InfoBg,
                error = Color(0xFFEF5350),
                onError = Color.White,
                errorContainer = Color(0xFF5D1F1F),
                onErrorContainer = ErrorBg,
                background = BackgroundDark,
                onBackground = OnSurfaceDark,
                surface = SurfaceDark,
                onSurface = OnSurfaceDark,
                surfaceVariant = SurfaceVariantDark,
                onSurfaceVariant = TextSecondaryDark,
                outline = BorderNormalDark,
                outlineVariant = Color(0xFF303030),
                scrim = Scrim
        )

/** Extended colors that are not part of Material3 color scheme. */
data class ExtendedColors(
        val success: Color,
        val successBg: Color,
        val successText: Color,
        val warning: Color,
        val warningBg: Color,
        val warningText: Color,
        val confidenceHigh: Color,
        val confidenceHighBg: Color,
        val confidenceMedium: Color,
        val confidenceMediumBg: Color,
        val confidenceLow: Color,
        val confidenceLowBg: Color,
        val navSelected: Color,
        val navUnselected: Color,
        val textPrimary: Color,
        val textSecondary: Color,
        val textTertiary: Color,
        val borderNormal: Color,
        val borderLight: Color,
        val surfaceTint: Color,
        val overlayLight: Color,
        val overlayDark: Color
)

private val LightExtendedColors =
        ExtendedColors(
                success = Success,
                successBg = SuccessBg,
                successText = SuccessText,
                warning = Warning,
                warningBg = WarningBg,
                warningText = WarningText,
                confidenceHigh = ConfidenceHigh,
                confidenceHighBg = ConfidenceHighBg,
                confidenceMedium = ConfidenceMedium,
                confidenceMediumBg = ConfidenceMediumBg,
                confidenceLow = ConfidenceLow,
                confidenceLowBg = ConfidenceLowBg,
                navSelected = NavSelected,
                navUnselected = NavUnselected,
                textPrimary = TextPrimary,
                textSecondary = TextSecondary,
                textTertiary = TextTertiary,
                borderNormal = BorderNormal,
                borderLight = BorderLight,
                surfaceTint = SurfaceTint,
                overlayLight = OverlayLight,
                overlayDark = OverlayDark
        )

private val DarkExtendedColors =
        ExtendedColors(
                success = Success,
                successBg = Color(0xFF1B3B2F),
                successText = Color(0xFF81C784),
                warning = Warning,
                warningBg = Color(0xFF3D2E1F),
                warningText = Color(0xFFFFB74D),
                confidenceHigh = ConfidenceHigh,
                confidenceHighBg = Color(0xFF1B3B2F),
                confidenceMedium = ConfidenceMedium,
                confidenceMediumBg = Color(0xFF3D2E1F),
                confidenceLow = Color(0xFFEF5350),
                confidenceLowBg = Color(0xFF5D1F1F),
                navSelected = PrimaryDarkTheme,
                navUnselected = Color(0xFF6B6B6B),
                textPrimary = TextPrimaryDark,
                textSecondary = TextSecondaryDark,
                textTertiary = Color(0xFF808080),
                borderNormal = BorderNormalDark,
                borderLight = Color(0xFF303030),
                surfaceTint = Color(0xFF1A2E24),
                overlayLight = Color(0x40FFFFFF),
                overlayDark = Color(0x80000000)
        )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/**
 * Main theme composable for EcoLens.
 *
 * @param darkTheme Whether to use dark theme
 * @param content The content to display with this theme
 */
@Composable
fun EcoLensTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
                colorScheme = colorScheme,
                typography = EcoLensTypography,
                shapes = EcoLensShapes,
                content = content
        )
    }
}

/** Access extended colors from the current theme. */
object EcoLensTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
