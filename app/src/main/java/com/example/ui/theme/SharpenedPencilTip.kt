package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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
