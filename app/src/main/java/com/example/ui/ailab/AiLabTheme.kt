package com.example.ui.ailab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PencilTheme

/**
 * Maps a card's [AiLabAccent] onto the active theme.
 *
 * The accents are declared once in the catalog and resolved here so the Lab
 * cards read correctly on both the light sketchbook and the dark studio theme.
 */
@Composable
@ReadOnlyComposable
fun AiLabAccent.color(): Color {
    val colors = PencilTheme.colors
    return when (this) {
        AiLabAccent.CYAN -> colors.accentCyan
        AiLabAccent.BLUE -> if (colors.isLight) Color(0xFF1A73E8) else Color(0xFF64B5F6)
        AiLabAccent.PURPLE -> colors.accentPurple
        AiLabAccent.PINK -> colors.accentPink
        AiLabAccent.GREEN -> colors.accentGreen
        AiLabAccent.YELLOW -> if (colors.isLight) Color(0xFFF29900) else colors.accentYellow
        AiLabAccent.TEAL -> if (colors.isLight) Color(0xFF00897B) else colors.accentTeal
        AiLabAccent.RED -> colors.accentRed
    }
}

/** A translucent wash of the accent for the icon tile behind a card. */
@Composable
@ReadOnlyComposable
fun AiLabAccent.soft(): Color =
    color().copy(alpha = if (PencilTheme.colors.isLight) 0.12f else 0.16f)
