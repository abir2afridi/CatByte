package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

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
 * Hand-drawn border for cards, buttons, and panels.
 */
fun Modifier.sketchPencilBorder(
    strokeWidth: Dp = 1.5.dp,
    color: Color = Color(0xFF202124),
    secondaryColor: Color = Color(0x59202124),
    cornerRadius: Dp = 12.dp
): Modifier = this.drawWithContent {
    drawContent()

    val primaryWidth = strokeWidth.toPx()
    val secondaryWidth = (strokeWidth * 0.7f).toPx()
    val rPx = cornerRadius.toPx()

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

/**
 * Diagonal 45-degree hatching pattern
 * Used to shade timeline clips, project cards, and active badges.
 */
fun Modifier.pencilHatching(
    color: Color = Color(0x1A202124),
    spacing: Dp = 8.dp,
    strokeWidth: Dp = 1.2.dp
): Modifier = this.drawBehind {
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
 * Spiral binding at top of screen or card
 */
@Composable
fun SpiralNotebookBinding(
    modifier: Modifier = Modifier,
    ringsCount: Int = 14,
    ringColor: Color = PencilTheme.colors.border.copy(alpha = 0.75f),
    holeColor: Color = Color(0x33000000)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
            .padding(horizontal = 12.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val step = size.width / (ringsCount + 1)
            for (i in 1..ringsCount) {
                val cx = i * step
                val cy = size.height / 2f

                drawCircle(
                    color = holeColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(cx, cy)
                )

                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(cx - 2.dp.toPx(), cy - 7.dp.toPx()),
                    size = Size(4.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

/**
 * Masking Tape / Washi Tape corner sticker
 */
@Composable
fun WashiTapeSticker(
    modifier: Modifier = Modifier,
    text: String? = null,
    color: Color = Color(0xD8F0E2A8),
    tapeBorderColor: Color = Color(0x558C733E)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .sketchPencilBorder(strokeWidth = 0.8.dp, color = tapeBorderColor, cornerRadius = 3.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!text.isNullOrEmpty()) {
            Text(
                text = text,
                color = PencilTheme.colors.textPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Draft Crop Corners (┌ ┐ └ ┘)
 */
@Composable
fun DraftingCropCorners(
    modifier: Modifier = Modifier,
    strokeColor: Color = PencilTheme.colors.border,
    lineLength: Dp = 16.dp,
    strokeWidth: Dp = 2.dp
) {
    Canvas(modifier = modifier) {
        val len = lineLength.toPx()
        val sw = strokeWidth.toPx()
        val w = size.width
        val h = size.height

        drawLine(strokeColor, Offset(0f, 0f), Offset(len, 0f), sw)
        drawLine(strokeColor, Offset(0f, 0f), Offset(0f, len), sw)

        drawLine(strokeColor, Offset(w - len, 0f), Offset(w, 0f), sw)
        drawLine(strokeColor, Offset(w, 0f), Offset(w, len), sw)

        drawLine(strokeColor, Offset(0f, h), Offset(len, h), sw)
        drawLine(strokeColor, Offset(0f, h - len), Offset(0f, h), sw)

        drawLine(strokeColor, Offset(w - len, h), Offset(w, h), sw)
        drawLine(strokeColor, Offset(w, h - len), Offset(w, h), sw)
    }
}

/**
 * Realistic Sharpened Pencil Indicator
 */
@Composable
fun SharpenedPencilTip(
    modifier: Modifier = Modifier,
    bodyColor: Color = Color(0xFFF59E0B),
    woodColor: Color = Color(0xFFE8C59A),
    leadColor: Color = Color(0xFF2C2B28)
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 24.dp)) {
        val w = size.width
        val h = size.height

        val bodyHeight = h * 0.45f
        drawRect(
            color = bodyColor,
            topLeft = Offset(1.5f, 0f),
            size = Size(w - 3f, bodyHeight)
        )
        drawRect(
            color = leadColor,
            topLeft = Offset(1.5f, 0f),
            size = Size(w - 3f, bodyHeight),
            style = Stroke(width = 1.2f)
        )

        val conePath = Path().apply {
            moveTo(1.5f, bodyHeight)
            lineTo(w - 1.5f, bodyHeight)
            lineTo(w / 2f, h)
            close()
        }
        drawPath(path = conePath, color = woodColor)
        drawPath(path = conePath, color = leadColor, style = Stroke(width = 1.2f))

        val leadPath = Path().apply {
            val tipStart = bodyHeight + (h - bodyHeight) * 0.55f
            val tipW = (w - 3f) * 0.45f
            moveTo((w - tipW) / 2f, tipStart)
            lineTo((w + tipW) / 2f, tipStart)
            lineTo(w / 2f, h)
            close()
        }
        drawPath(path = leadPath, color = leadColor)
    }
}

/**
 * Torn Paper Edge - Renders an organic ripped/serrated edge.
 */
@Composable
fun TornPaperEdge(
    modifier: Modifier = Modifier,
    teethCount: Int = 26,
    paperColor: Color = PencilTheme.colors.surface,
    borderColor: Color = PencilTheme.colors.borderSubtle
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val w = size.width
        val h = size.height
        val step = w / teethCount.toFloat()

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h * 0.3f)
            var currentX = 0f
            for (i in 0 until teethCount) {
                val midX = currentX + step * 0.5f
                val nextX = currentX + step
                val dipY = if (i % 2 == 0) h else h * 0.75f
                val riseY = if (i % 3 == 0) h * 0.2f else h * 0.35f
                lineTo(midX, dipY)
                lineTo(nextX, riseY)
                currentX = nextX
            }
            lineTo(w, 0f)
            close()
        }

        drawPath(path = path, color = paperColor)
        drawPath(path = path, color = borderColor, style = Stroke(width = 1f))
    }
}

/**
 * Corner Masking / Washi Tape with pinking-shear serrated ends
 */
@Composable
fun WashiTapeCorner(
    modifier: Modifier = Modifier,
    rotationDegrees: Float = -12f,
    widthDp: Dp = 52.dp,
    heightDp: Dp = 18.dp,
    tapeColor: Color = Color(0xDDF0E3A2),
    tapeLabel: String? = null
) {
    Box(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .offset(y = (-4).dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val w = size.width
            val h = size.height

            val tapePath = Path().apply {
                moveTo(3.dp.toPx(), 0f)
                val teeth = 4
                val toothH = h / teeth
                for (i in 0 until teeth) {
                    val y1 = i * toothH + toothH * 0.5f
                    val y2 = (i + 1) * toothH
                    lineTo(0f, y1)
                    lineTo(3.dp.toPx(), y2)
                }

                lineTo(w - 3.dp.toPx(), h)

                for (i in teeth downTo 1) {
                    val y1 = i * toothH - toothH * 0.5f
                    val y2 = (i - 1) * toothH
                    lineTo(w, y1)
                    lineTo(w - 3.dp.toPx(), y2)
                }

                close()
            }

            drawPath(path = tapePath, color = tapeColor)
            drawPath(path = tapePath, color = Color(0x334A3E26), style = Stroke(width = 0.8f))
        }

        if (!tapeLabel.isNullOrBlank()) {
            Text(
                text = tapeLabel,
                color = PencilTheme.colors.textPrimary,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Sketched Curved Steel Wire Paperclip
 */
@Composable
fun MetalPaperclip(
    modifier: Modifier = Modifier,
    clipColor: Color = Color(0xFF6B7280),
    shadowColor: Color = Color(0x33000000)
) {
    Canvas(modifier = modifier.size(width = 14.dp, height = 30.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.8.dp.toPx()

        val shadowPath = Path().apply {
            moveTo(w * 0.28f + 1f, h * 0.85f + 1f)
            lineTo(w * 0.28f + 1f, h * 0.35f + 1f)
            cubicTo(w * 0.28f + 1f, 1f, w * 0.85f + 1f, 1f, w * 0.85f + 1f, h * 0.35f + 1f)
            lineTo(w * 0.85f + 1f, h * 0.72f + 1f)
            cubicTo(w * 0.85f + 1f, h + 1f, w * 0.50f + 1f, h + 1f, w * 0.50f + 1f, h * 0.72f + 1f)
            lineTo(w * 0.50f + 1f, h * 0.45f + 1f)
        }
        drawPath(path = shadowPath, color = shadowColor, style = Stroke(width = strokePx))

        val wirePath = Path().apply {
            moveTo(w * 0.28f, h * 0.85f)
            lineTo(w * 0.28f, h * 0.35f)
            cubicTo(w * 0.28f, 0f, w * 0.85f, 0f, w * 0.85f, h * 0.35f)
            lineTo(w * 0.85f, h * 0.72f)
            cubicTo(w * 0.85f, h, w * 0.50f, h, w * 0.50f, h * 0.72f)
            lineTo(w * 0.50f, h * 0.45f)
        }
        drawPath(path = wirePath, color = clipColor, style = Stroke(width = strokePx))
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