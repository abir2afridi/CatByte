package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// Pencil & Paper Light Palette (Notebook Parchment & 2B Graphite)
// ==========================================
val PaperBackgroundLight = Color(0xFFF7F3E8)     // Textured sketchbook cream parchment
val PaperSurfaceLight = Color(0xFFFFFDF8)        // Fresh drawing paper
val PaperSurfaceVariantLight = Color(0xFFEFE8D8) // Warm sketch card layer
val PaperSurfaceElevatedLight = Color(0xFFE6DEC9) // Pressed kraft paper pill
val PaperSurfaceGlassLight = Color(0xEEF8F4EA)
val PencilBorderLight = Color(0xFF2C2B28)        // Sharp graphite pencil outline
val PencilBorderSubtleLight = Color(0xFFD6CEBD)  // Faint notebook grid line

val PencilTextPrimaryLight = Color(0xFF1F1D1A)   // Dark 2B graphite core
val PencilTextSecondaryLight = Color(0xFF5D5850) // Soft shading lead
val PencilTextTertiaryLight = Color(0xFF8E877C)  // Light pencil draft guide

val PencilAccentCyanLight = Color(0xFF0284C7)    // Blueprint cyan pencil
val PencilAccentRedLight = Color(0xFFD90429)     // Red editing pencil
val PencilAccentYellowLight = Color(0xFFD97706)  // Ochre yellow crayon
val PencilAccentPurpleLight = Color(0xFF7B2CBF)  // Violet colored pencil
val PencilAccentGreenLight = Color(0xFF059669)   // Forest green drafting pencil
val PencilAccentPinkLight = Color(0xFFDB2777)    // Magenta highlighter
val PencilAccentTealLight = Color(0xFF0D9488)

val PencilPlayheadLight = Color(0xFF1F1D1A)
val PencilPlayheadGlowLight = Color(0xFF0284C7)
val PencilTimelineTrackLight = Color(0xFFECE4D2)
val PencilClipBgLight = Color(0xFFE0D7C3)
val PencilClipSelectedLight = Color(0xFFD2C7B0)
val PencilHandleWhiteLight = Color(0xFF1F1D1A)

// ==========================================
// Charcoal Slate Dark Palette (Chalk on Black Sketchpad)
// ==========================================
val SlateBackgroundDark = Color(0xFF141519)      // Charcoal drawing pad
val SlateSurfaceDark = Color(0xFF1C1D24)         // Matte slate studio deck
val SlateSurfaceVariantDark = Color(0xFF262732)  // Graphite shade card
val SlateSurfaceElevatedDark = Color(0xFF323442) // Elevated chalk block
val SlateSurfaceGlassDark = Color(0xDD1B1C23)
val ChalkBorderDark = Color(0xFF4C4E61)          // White chalk sketch border
val ChalkBorderSubtleDark = Color(0xFF2B2D3A)    // Blueprint grid

val ChalkTextPrimaryDark = Color(0xFFF6F6F9)     // Pure chalk white
val ChalkTextSecondaryDark = Color(0xFFA5A9B8)   // Faded pencil sketch
val ChalkTextTertiaryDark = Color(0xFF6B7082)    // Charcoal shadow

val ChalkAccentCyanDark = Color(0xFF38BDF8)      // Electric blue chalk
val ChalkAccentRedDark = Color(0xFFFF4D6D)       // Red chalk mark
val ChalkAccentYellowDark = Color(0xFFFFB703)    // Yellow pastel
val ChalkAccentPurpleDark = Color(0xFFA855F7)    // Lavender pastel
val ChalkAccentGreenDark = Color(0xFF34D399)     // Mint pastel
val ChalkAccentPinkDark = Color(0xFFF472B6)      // Pink pastel
val ChalkAccentTealDark = Color(0xFF2DD4BF)

val ChalkPlayheadDark = Color(0xFFFFFFFF)
val ChalkPlayheadGlowDark = Color(0xFF38BDF8)
val ChalkTimelineTrackDark = Color(0xFF181A20)
val ChalkClipBgDark = Color(0xFF282A36)
val ChalkClipSelectedDark = Color(0xFF373A4C)
val ChalkHandleWhiteDark = Color(0xFFFFFFFF)

// ==========================================
// Semantic Pencil Theme System
// ==========================================
data class PencilPaperColors(
    val isLight: Boolean,
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

val LightPencilPaperColors = PencilPaperColors(
    isLight = true,
    background = PaperBackgroundLight,
    surface = PaperSurfaceLight,
    surfaceVariant = PaperSurfaceVariantLight,
    surfaceElevated = PaperSurfaceElevatedLight,
    border = PencilBorderLight,
    borderSubtle = PencilBorderSubtleLight,
    textPrimary = PencilTextPrimaryLight,
    textSecondary = PencilTextSecondaryLight,
    textTertiary = PencilTextTertiaryLight,
    accentCyan = PencilAccentCyanLight,
    accentRed = PencilAccentRedLight,
    accentYellow = PencilAccentYellowLight,
    accentPurple = PencilAccentPurpleLight,
    accentGreen = PencilAccentGreenLight,
    accentPink = PencilAccentPinkLight,
    playhead = PencilPlayheadLight,
    timelineTrack = PencilTimelineTrackLight,
    clipBg = PencilClipBgLight,
    clipSelected = PencilClipSelectedLight,
    handle = PencilHandleWhiteLight
)

val DarkPencilPaperColors = PencilPaperColors(
    isLight = false,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    surfaceElevated = SlateSurfaceElevatedDark,
    border = ChalkBorderDark,
    borderSubtle = ChalkBorderSubtleDark,
    textPrimary = ChalkTextPrimaryDark,
    textSecondary = ChalkTextSecondaryDark,
    textTertiary = ChalkTextTertiaryDark,
    accentCyan = ChalkAccentCyanDark,
    accentRed = ChalkAccentRedDark,
    accentYellow = ChalkAccentYellowDark,
    accentPurple = ChalkAccentPurpleDark,
    accentGreen = ChalkAccentGreenDark,
    accentPink = ChalkAccentPinkDark,
    playhead = ChalkPlayheadDark,
    timelineTrack = ChalkTimelineTrackDark,
    clipBg = ChalkClipBgDark,
    clipSelected = ChalkClipSelectedDark,
    handle = ChalkHandleWhiteDark
)

val LocalPencilPaperColors = staticCompositionLocalOf { LightPencilPaperColors }

object PencilTheme {
    val colors: PencilPaperColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPencilPaperColors.current
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

