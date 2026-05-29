package com.example.newsapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

private val DarkColorScheme = darkColorScheme(
    primary             = PrimaryDark,
    onPrimary           = OnPrimaryDark,
    primaryContainer    = PrimaryContainerDark,
    onPrimaryContainer  = OnPrimaryContainerDark,
    secondary           = SecondaryDark,
    onSecondary         = OnSecondaryDark,
    secondaryContainer  = SecondaryContainerDark,
    onSecondaryContainer= OnSecondaryContainerDark,
    tertiary            = TertiaryDark,
    tertiaryContainer   = TertiaryContainerDark,
    background          = BackgroundDark,
    onBackground        = OnBackgroundDark,
    surface             = SurfaceDark,
    onSurface           = OnSurfaceDark,
    surfaceVariant      = SurfaceContainerHighDark,
    onSurfaceVariant    = OnSurfaceVariantDark,
    surfaceContainerLowest  = SurfaceContainerLowestDark,
    surfaceContainerLow     = SurfaceContainerLowDark,
    surfaceContainer        = SurfaceContainerDark,
    surfaceContainerHigh    = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    outline             = OutlineDark,
    outlineVariant      = OutlineVariantDark,
    error               = ErrorDark,
    onError             = OnErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary             = PrimaryLight,
    onPrimary           = OnPrimaryLight,
    primaryContainer    = PrimaryContainerLight,
    onPrimaryContainer  = OnPrimaryContainerLight,
    secondary           = SecondaryLight,
    onSecondary         = OnSecondaryLight,
    secondaryContainer  = SecondaryContainerLight,
    onSecondaryContainer= OnSecondaryContainerLight,
    tertiary            = TertiaryLight,
    tertiaryContainer   = TertiaryContainerLight,
    background          = BackgroundLight,
    onBackground        = OnBackgroundLight,
    surface             = SurfaceLight,
    onSurface           = OnSurfaceLight,
    surfaceVariant      = SurfaceContainerHighLight,
    onSurfaceVariant    = OnSurfaceVariantLight,
    surfaceContainerLowest  = SurfaceContainerLowestLight,
    surfaceContainerLow     = SurfaceContainerLowLight,
    surfaceContainer        = SurfaceContainerLight,
    surfaceContainerHigh    = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    outline             = OutlineLight,
    outlineVariant      = OutlineVariantLight,
    error               = ErrorLight,
    onError             = OnErrorLight,
)

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    secondary = Color(0xFFCCCCCC),
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = Color(0xFFFF5555)
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    secondary = Color(0xFF333333),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6E6E6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE6E6E6),
    surfaceContainerHighest = Color(0xFFDDDDDD),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF1A1A1A),
    error = Color(0xFFCC0000)
)

@Composable
fun NewsAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disabled by default to preserve the strict Editorial Design Foundation
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast && darkTheme -> HighContrastDarkColorScheme
        highContrast && !darkTheme -> HighContrastLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val density = LocalDensity.current
    val dynamicTypography = androidx.compose.material3.Typography(
        displayLarge = Typography.displayLarge.nonLinearScale(density, 1.2f),
        displayMedium = Typography.displayMedium.nonLinearScale(density, 1.2f),
        displaySmall = Typography.displaySmall.nonLinearScale(density, 1.2f),
        headlineLarge = Typography.headlineLarge.nonLinearScale(density, 1.3f),
        headlineMedium = Typography.headlineMedium.nonLinearScale(density, 1.3f),
        headlineSmall = Typography.headlineSmall.nonLinearScale(density, 1.3f),
        titleLarge = Typography.titleLarge.nonLinearScale(density, 1.3f),
        titleMedium = Typography.titleMedium.nonLinearScale(density, 1.3f),
        titleSmall = Typography.titleSmall.nonLinearScale(density, 1.3f),
        bodyLarge = Typography.bodyLarge,
        bodyMedium = Typography.bodyMedium,
        bodySmall = Typography.bodySmall,
        labelLarge = Typography.labelLarge,
        labelMedium = Typography.labelMedium,
        labelSmall = Typography.labelSmall
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        content = content
    )
}