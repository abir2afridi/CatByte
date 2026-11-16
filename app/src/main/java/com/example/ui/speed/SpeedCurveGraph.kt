package com.example.ui.speed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BezierPoint
import com.example.ui.theme.PencilTheme

internal const val CURVE_GRAPH_WIDTH = 300f
internal const val CURVE_GRAPH_HEIGHT = 200f

private const val CURVE_GRID_ROWS = 8
private const val CURVE_GRID_COLS = 10

/** Speed curve graph: grid, curve path, control points and speed labels. */
@Composable
internal fun SpeedCurveGraph(
    points: List<BezierPoint>,
    selectedPointIndex: Int?
) {
    val colors = PencilTheme.colors

    val path = remember(points) {
        buildCurvePath(points, CURVE_GRAPH_WIDTH, CURVE_GRAPH_HEIGHT)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .border(1.5.dp, colors.border, RoundedCornerShape(12.dp))
            .testTag("speed_curve_graph")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // Grid Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stepX = w / CURVE_GRID_COLS
                val stepY = h / CURVE_GRID_ROWS

                // Subtle grid lines
                for (i in 0..CURVE_GRID_COLS) {
                    val x = i * stepX
                    drawLine(
                        color = colors.borderSubtle.copy(alpha = 0.3f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 0.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }
                for (i in 0..CURVE_GRID_ROWS) {
                    val y = i * stepY
                    drawLine(
                        color = colors.borderSubtle.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 0.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                }

                // Center line (1x speed)
                val centerY = h / 2f
                drawLine(
                    color = colors.accentCyan.copy(alpha = 0.5f),
                    start = Offset(0f, centerY),
                    end = Offset(w, centerY),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            // Curve Path
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPath(
                    path = path,
                    color = colors.accentCyan
                )

                // Fill area under curve
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    color = colors.accentCyan.copy(alpha = 0.1f)
                )
            }

            // Control Points as clickable boxes
            points.forEachIndexed { index, point ->
                val x = point.x * CURVE_GRAPH_WIDTH
                val normalizedSpeed = point.speedMultiplier.coerceIn(0.1f, 3f)
                val speedRatio = (normalizedSpeed - 0.1f) / 2.9f
                val y = (1f - speedRatio) * CURVE_GRAPH_HEIGHT

                val isSelected = selectedPointIndex == index

                Box(
                    modifier = Modifier
                        .padding((x - 12).dp, (y - 12).dp, 0.dp, 0.dp)
                        .size(24.dp)
                        .fillMaxSize()
                ) {
                    // Control Point Visual
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(if (isSelected) colors.accentRed else colors.accentCyan, CircleShape)
                            .border(2.dp, if (isSelected) colors.accentRed else colors.accentCyan, CircleShape)
                    ) {
                        // Index badge
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(if (index == 0 || index == points.lastIndex) colors.accentCyan else colors.textTertiary, CircleShape)
                                .padding(2.dp)
                        )
                    }
                }
            }

            // Speed labels
            Column(
                modifier = Modifier
                    .padding(top = 8.dp, start = 8.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text("3.0x", color = colors.accentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("1.0x", color = colors.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("0.1x", color = colors.accentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
