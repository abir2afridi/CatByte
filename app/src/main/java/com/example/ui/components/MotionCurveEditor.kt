package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.BezierPoint
import com.example.ui.theme.PencilTheme

/**
 * Interactive Bézier Motion & Velocity Curve Editor Canvas.
 */
@Composable
fun MotionCurveEditor(
    points: List<BezierPoint>,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw grid lines
            for (i in 1..4) {
                val y = h * (i / 5f)
                drawLine(colors.borderSubtle, Offset(0f, y), Offset(w, y), 1f)
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    val first = points.first()
                    moveTo(first.x * w, (1f - (first.speedMultiplier / 5f).coerceIn(0f, 1f)) * h)
                    for (i in 1 until points.size) {
                        val pt = points[i]
                        lineTo(pt.x * w, (1f - (pt.speedMultiplier / 5f).coerceIn(0f, 1f)) * h)
                    }
                }
                drawPath(path, colors.accentCyan, style = Stroke(width = 3f))

                for (pt in points) {
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = Offset(pt.x * w, (1f - (pt.speedMultiplier / 5f).coerceIn(0f, 1f)) * h)
                    )
                }
            }
        }
    }
}
