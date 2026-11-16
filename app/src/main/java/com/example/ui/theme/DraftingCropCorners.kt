package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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
