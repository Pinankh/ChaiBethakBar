package com.pinankh.chaibethakbar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = EspressoPrimary,
    onPrimary = EspressoOnPrimary,
    primaryContainer = EspressoPrimaryContainer,
    onPrimaryContainer = EspressoOnPrimaryContainer,
    secondary = AmberSecondary,
    onSecondary = AmberOnSecondary,
    secondaryContainer = AmberSecondaryContainer,
    onSecondaryContainer = AmberOnSecondaryContainer,
    tertiary = WarmOliveTertiary,
    onTertiary = WarmOliveOnTertiary,
    tertiaryContainer = WarmOliveTertiaryContainer,
    onTertiaryContainer = WarmOliveOnTertiaryContainer,
    background = PosBackgroundLight,
    onBackground = PosOnBackgroundLight,
    surface = PosSurfaceLight,
    onSurface = PosOnSurfaceLight,
    surfaceVariant = PosSurfaceVariantLight,
    onSurfaceVariant = PosOnSurfaceVariantLight,
    outline = PosOutlineLight,
    outlineVariant = PosOutlineVariantLight,
    error = StatusCancelledRed,
    onError = StatusOnCancelledContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = EspressoPrimaryDark,
    onPrimary = EspressoOnPrimaryDark,
    primaryContainer = EspressoPrimaryContainerDark,
    onPrimaryContainer = EspressoOnPrimaryContainerDark,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    onSecondaryContainer = AmberOnSecondaryContainerDark,
    background = PosBackgroundDark,
    onBackground = PosOnBackgroundDark,
    surface = PosSurfaceDark,
    onSurface = PosOnSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurfaceVariant = PosOnSurfaceVariantDark,
    outline = PosOutlineDark,
    error = StatusCancelledRed,
    onError = StatusOnCancelledContainer
)

@Composable
fun ChaiBethakBarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
