package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val title: String, val subtitle: String) {
    PENCIL_LIGHT("Pencil on Paper (Light)", "Warm sketchbook parchment with 2B graphite"),
    CHARCOAL_DARK("Charcoal Slate (Dark)", "Dark chalkboard with chalk sketch lines"),
    SYSTEM("Follow System", "Match Android device appearance")
}

private val LightPaperMaterialScheme = lightColorScheme(
    primary = PencilAccentCyanLight,
    onPrimary = Color.White,
    primaryContainer = PaperSurfaceElevatedLight,
    onPrimaryContainer = PencilTextPrimaryLight,
    secondary = PencilAccentPurpleLight,
    onSecondary = Color.White,
    secondaryContainer = PaperSurfaceVariantLight,
    onSecondaryContainer = PencilTextPrimaryLight,
    tertiary = PencilAccentGreenLight,
    onTertiary = Color.White,
    background = PaperBackgroundLight,
    onBackground = PencilTextPrimaryLight,
    surface = PaperSurfaceLight,
    onSurface = PencilTextPrimaryLight,
    surfaceVariant = PaperSurfaceVariantLight,
    onSurfaceVariant = PencilTextSecondaryLight,
    outline = PencilBorderLight,
    error = PencilAccentRedLight,
    onError = Color.White
)

private val DarkSlateMaterialScheme = darkColorScheme(
    primary = ChalkAccentCyanDark,
    onPrimary = Color.Black,
    primaryContainer = SlateSurfaceElevatedDark,
    onPrimaryContainer = ChalkTextPrimaryDark,
    secondary = ChalkAccentPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = SlateSurfaceVariantDark,
    onSecondaryContainer = ChalkTextPrimaryDark,
    tertiary = ChalkAccentGreenDark,
    onTertiary = Color.Black,
    background = SlateBackgroundDark,
    onBackground = ChalkTextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = ChalkTextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = ChalkTextSecondaryDark,
    outline = ChalkBorderDark,
    error = ChalkAccentRedDark,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.PENCIL_LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.PENCIL_LIGHT -> false
        AppThemeMode.CHARCOAL_DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val pencilColors = if (isDark) DarkPencilPaperColors else LightPencilPaperColors
    val materialScheme = if (isDark) DarkSlateMaterialScheme else LightPaperMaterialScheme

    CompositionLocalProvider(LocalPencilPaperColors provides pencilColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = Typography,
            content = content
        )
    }
}
