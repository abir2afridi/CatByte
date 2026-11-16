package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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
