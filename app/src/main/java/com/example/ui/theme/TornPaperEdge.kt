package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

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
