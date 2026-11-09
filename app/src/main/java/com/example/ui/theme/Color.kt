package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Light Palette (Clean Professional)
// ==========================================
val LightBackground = Color(0xFFF8F9FA)        // Clean light background
val LightSurface = Color(0xFFFFFFFF)           // Pure white surface
val LightSurfaceVariant = Color(0xFFF1F3F4)    // Subtle card layer
val LightSurfaceElevated = Color(0xFFE8EAED)   // Elevated card
val LightSurfaceGlass = Color(0xEEF8F9FA)
val LightBorder = Color(0xFF202124)            // Primary outline
val LightBorderSubtle = Color(0xFFDADCE0)      // Subtle divider

val LightTextPrimary = Color(0xFF202124)       // Primary text
val LightTextSecondary = Color(0xFF5F6368)     // Secondary text
val LightTextTertiary = Color(0xFF80868B)      // Tertiary text

val LightAccentCyan = Color(0xFF1A73E8)        // Primary cyan
val LightAccentRed = Color(0xFFD93025)         // Red accent
val LightAccentYellow = Color(0xFFF29900)      // Yellow accent
val LightAccentPurple = Color(0xFF8E24AA)      // Purple accent
val LightAccentGreen = Color(0xFF34A853)       // Green accent
val LightAccentPink = Color(0xFFC2185B)        // Pink accent
val LightAccentTeal = Color(0xFF009688)        // Teal accent

val LightPlayhead = Color(0xFF1A73E8)
val LightPlayheadGlow = Color(0xFF1A73E8)
val LightTimelineTrack = Color(0xFFE8EAED)
val LightClipBg = Color(0xFFF1F3F4)
val LightClipSelected = Color(0xFFE8EAED)
val LightHandle = Color(0xFF1A73E8)

// ==========================================
// Dark Palette (CapCut Professional Dark Studio)
// ==========================================
val DarkBackground = Color(0xFF0A0A0E)         // Pitch OLED studio dark
val DarkSurface = Color(0xFF18191C)            // Charcoal studio surface
val DarkSurfaceVariant = Color(0xFF22242A)     // Dark card layer
val DarkSurfaceElevated = Color(0xFF2D3038)    // Elevated studio card
val DarkSurfaceGlass = Color(0xEE121318)
val DarkBorder = Color(0xFF383A44)             // Subtle studio outline
val DarkBorderSubtle = Color(0xFF262830)       // Subtle divider

val DarkTextPrimary = Color(0xFFFFFFFF)        // Pure white text
val DarkTextSecondary = Color(0xFFA1A5B0)      // Secondary studio text
val DarkTextTertiary = Color(0xFF6A6E79)       // Muted gray text

val DarkAccentCyan = Color(0xFF00F2FE)         // CapCut Electric Cyan
val DarkAccentRed = Color(0xFFFF4D4D)          // Bright studio red
val DarkAccentYellow = Color(0xFFFFC107)       // Bright yellow
val DarkAccentPurple = Color(0xFFB388FF)       // Bright studio purple
val DarkAccentGreen = Color(0xFF00E676)        // Bright studio green
val DarkAccentPink = Color(0xFFFF4081)         // Bright studio pink
val DarkAccentTeal = Color(0xFF00E5FF)         // CapCut Neon Teal

val DarkPlayhead = Color(0xFFFFFFFF)           // Fixed Steel White Needle
val DarkPlayheadGlow = Color(0xFF00F2FE)       // Cyan glow
val DarkTimelineTrack = Color(0xFF121317)      // Dark track slot
val DarkClipBg = Color(0xFF1E2026)             // Track clip card
val DarkClipSelected = Color(0xFF282A32)         // Selected clip background
val DarkHandle = Color(0xFF00F2FE)             // CapCut Cyan trim handle

// ==========================================
// Semantic Theme System
// ==========================================
data class AppColors(
    val isLight: Boolean,
    val isPencilSketch: Boolean = false,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accentCyan: Color,
    val accentRed: Color,
    val accentYellow: Color,
    val accentPurple: Color,
    val accentGreen: Color,
    val accentPink: Color,
    val playhead: Color,
    val timelineTrack: Color,
    val clipBg: Color,
    val clipSelected: Color,
    val handle: Color
)

val LightAppColors = AppColors(
    isLight = true,
    isPencilSketch = true,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceElevated = LightSurfaceElevated,
    border = LightBorder,
    borderSubtle = LightBorderSubtle,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textTertiary = LightTextTertiary,
    accentCyan = LightAccentCyan,
    accentRed = LightAccentRed,
    accentYellow = LightAccentYellow,
    accentPurple = LightAccentPurple,
    accentGreen = LightAccentGreen,
    accentPink = LightAccentPink,
    playhead = LightPlayhead,
    timelineTrack = LightTimelineTrack,
    clipBg = LightClipBg,
    clipSelected = LightClipSelected,
    handle = LightHandle
)

val DarkAppColors = AppColors(
    isLight = false,
    isPencilSketch = false,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceElevated = DarkSurfaceElevated,
    border = DarkBorder,
    borderSubtle = DarkBorderSubtle,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textTertiary = DarkTextTertiary,
    accentCyan = DarkAccentCyan,
    accentRed = DarkAccentRed,
    accentYellow = DarkAccentYellow,
    accentPurple = DarkAccentPurple,
    accentGreen = DarkAccentGreen,
    accentPink = DarkAccentPink,
    playhead = DarkPlayhead,
    timelineTrack = DarkTimelineTrack,
    clipBg = DarkClipBg,
    clipSelected = DarkClipSelected,
    handle = DarkHandle
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

object PencilTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// ==========================================
// Compatibility Aliases for CapCut UI
// ==========================================
val CapCutDarkBackground: Color @Composable get() = PencilTheme.colors.background
val CapCutSurface: Color @Composable get() = PencilTheme.colors.surface
val CapCutSurfaceVariant: Color @Composable get() = PencilTheme.colors.surfaceVariant
val CapCutSurfaceElevated: Color @Composable get() = PencilTheme.colors.surfaceElevated
val CapCutBorder: Color @Composable get() = PencilTheme.colors.border
val CapCutBorderSubtle: Color @Composable get() = PencilTheme.colors.borderSubtle
val CapCutTextPrimary: Color @Composable get() = PencilTheme.colors.textPrimary
val CapCutTextSecondary: Color @Composable get() = PencilTheme.colors.textSecondary
val CapCutCyan: Color @Composable get() = PencilTheme.colors.accentCyan
val CapCutRed: Color @Composable get() = PencilTheme.colors.accentRed
val CapCutYellow: Color @Composable get() = PencilTheme.colors.accentYellow
val CapCutPurple: Color @Composable get() = PencilTheme.colors.accentPurple
val CapCutGreen: Color @Composable get() = PencilTheme.colors.accentGreen
val CapCutPink: Color @Composable get() = PencilTheme.colors.accentPink