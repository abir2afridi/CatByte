package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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
