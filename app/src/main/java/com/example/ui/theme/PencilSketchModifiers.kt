package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Default colors for non-Composable contexts
private val DefaultGridColorLight = Color(0x1A202124)  // Light grid
private val DefaultGridColorDark = Color(0x1AE0E0E0)   // Dark grid
private val DefaultMarginColor = Color(0x33D93025)     // Red margin

/**
 * Modifier to paint subtle grid lines on any surface.
 */
fun Modifier.paperNotebookBackground(
    showGrid: Boolean = true,
    gridSizeDp: Dp = 22.dp,
    gridColor: Color = DefaultGridColorLight,
    marginLineColor: Color = DefaultMarginColor
): Modifier = this.drawBehind {
    if (!showGrid) return@drawBehind

    val stepPx = gridSizeDp.toPx()
    val width = size.width
    val height = size.height

    // 1. Draw subtle horizontal grid lines
    var y = stepPx
    while (y < height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += stepPx
    }

    // 2. Draw vertical grid lines
    var x = stepPx
    while (x < width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1f
        )
        x += stepPx
    }

    // 3. Draw subtle margin line on the left
    val marginPx = 28.dp.toPx()
    if (width > marginPx + 40f) {
        drawLine(
            color = marginLineColor,
            start = Offset(marginPx, 0f),
            end = Offset(marginPx, height),
            strokeWidth = 1.5f
        )
    }
}

/**
 * Hand-drawn or modern clean border for cards, buttons, and panels.
 */
fun Modifier.sketchPencilBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color = Color(0xFF202124),
    secondaryColor: Color = Color(0x59202124),
    cornerRadius: Dp = 12.dp,
    isPencilStyle: Boolean = true
): Modifier = this.drawWithContent {
    drawContent()

    val primaryWidth = strokeWidth.toPx()
    val rPx = cornerRadius.toPx()

    if (!isPencilStyle) {
        // Clean CapCut-style modern border
        drawRoundRect(
            color = color,
            topLeft = Offset(primaryWidth / 2f, primaryWidth / 2f),
            size = Size(size.width - primaryWidth, size.height - primaryWidth),
            cornerRadius = CornerRadius(rPx, rPx),
            style = Stroke(width = primaryWidth)
        )
    } else {
        val secondaryWidth = (strokeWidth * 0.7f).toPx()

        // 1. Primary border
        drawRoundRect(
            color = color,
            topLeft = Offset(primaryWidth / 2f, primaryWidth / 2f),
            size = Size(size.width - primaryWidth, size.height - primaryWidth),
            cornerRadius = CornerRadius(rPx, rPx),
            style = Stroke(width = primaryWidth)
        )

        // 2. Secondary trace line (slightly inset)
        drawRoundRect(
            color = secondaryColor,
            topLeft = Offset(primaryWidth + 1f, primaryWidth + 1f),
            size = Size(
                (size.width - (primaryWidth * 2f) - 2f).coerceAtLeast(0f),
                (size.height - (primaryWidth * 2f) - 2f).coerceAtLeast(0f)
            ),
            cornerRadius = CornerRadius((rPx - 2f).coerceAtLeast(0f), (rPx - 2f).coerceAtLeast(0f)),
            style = Stroke(width = secondaryWidth)
        )
    }
}

/**
 * Diagonal 45-degree hatching pattern
 * Used to shade timeline clips, project cards, and active badges.
 */
fun Modifier.pencilHatching(
    color: Color = Color(0x1A202124),
    spacing: Dp = 8.dp,
    strokeWidth: Dp = 1.2.dp,
    enabled: Boolean = true
): Modifier = this.drawBehind {
    if (!enabled) return@drawBehind

    val step = spacing.toPx()
    val strokePx = strokeWidth.toPx()
    val w = size.width
    val h = size.height
    val total = w + h

    var pos = 0f
    while (pos < total) {
        drawLine(
            color = color,
            start = Offset(pos, 0f),
            end = Offset(0f, pos),
            strokeWidth = strokePx
        )
        pos += step
    }
}

/**
 * Organic Sketch Border with overshooting corner cross ticks
 */
fun Modifier.draftsmanSketchBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color = Color(0xFF202124),
    crossOvershootDp: Dp = 5.dp
): Modifier = this.drawWithContent {
    drawContent()

    val sw = strokeWidth.toPx()
    val over = crossOvershootDp.toPx()
    val w = size.width
    val h = size.height

    drawLine(color = color, start = Offset(-over, 0f), end = Offset(w + over, 0f), strokeWidth = sw)
    drawLine(color = color, start = Offset(-over, h), end = Offset(w + over, h), strokeWidth = sw)
    drawLine(color = color, start = Offset(0f, -over), end = Offset(0f, h + over), strokeWidth = sw)
    drawLine(color = color, start = Offset(w, -over), end = Offset(w, h + over), strokeWidth = sw)

    val inset = 3.dp.toPx()
    val subtleColor = color.copy(alpha = 0.25f)
    drawRect(
        color = subtleColor,
        topLeft = Offset(inset, inset),
        size = Size(w - inset * 2, h - inset * 2),
        style = Stroke(width = 0.8f)
    )
}
