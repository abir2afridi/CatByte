package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
