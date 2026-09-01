package com.vh.health.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = NightBlue,
    onPrimary = SurfaceLight,
    secondary = Teal,
    onSecondary = SurfaceLight,
    tertiary = Ember,
    onTertiary = SurfaceLight,
    background = PaperLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    onSurfaceVariant = InkMutedLight,
    error = Ember,
)

private val DarkColors = darkColorScheme(
    primary = NightBlueLight,
    onPrimary = PaperDark,
    secondary = TealLight,
    onSecondary = PaperDark,
    tertiary = EmberLight,
    onTertiary = PaperDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    onSurfaceVariant = InkMutedDark,
    error = EmberLight,
)

@Composable
fun VhHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = VhTypography,
        content = content,
    )
}
