package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PencilTheme

/** Colours offered by the drawing tool. */
internal val DrawColors = listOf(
    0xFFFF3B30, // red
    0xFFFFC94A, // amber
    0xFF26E5D7, // cyan
    0xFF4CAF50, // green
    0xFFFFFFFF  // white
)

/**
 * Freehand drawing sheet ("Text → Draw").
 *
 * Drag inside the board to draw; Apply stores the stroke at the playhead so it
 * is burned into the preview and the exported file (both read
 * `VideoProject.strokes`). Points are normalised, so the drawing lands in the
 * same place at any output size.
 */
@Composable
fun DrawSheet(
    onApply: (points: List<Float>, colorArgb: Long, widthFraction: Float) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = PencilTheme.colors
    val points = remember { mutableStateListOf<Offset>() }
    var colorArgb by remember { mutableStateOf(DrawColors.first()) }
    var widthFraction by remember { mutableStateOf(0.012f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(12.dp)
            .testTag("draw_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Draw", color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Clear",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable {
                            points.clear()
                            onClearAll()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("draw_clear")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentCyan)
                        .clickable {
                            if (points.size >= 2) {
                                val flat = ArrayList<Float>(points.size * 2)
                                points.forEach { flat.add(it.x); flat.add(it.y) }
                                onApply(flat, colorArgb, widthFraction)
                                points.clear()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("draw_apply")
                ) {
                    Text("Apply", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Done",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable { onClose() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("draw_close")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drawing board: normalised points in, preview stroke out.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.background)
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            points.clear()
                            points.add(
                                Offset(
                                    (offset.x / size.width).coerceIn(0f, 1f),
                                    (offset.y / size.height).coerceIn(0f, 1f)
                                )
                            )
                        },
                        onDrag = { change, _ ->
                            points.add(
                                Offset(
                                    (change.position.x / size.width).coerceIn(0f, 1f),
                                    (change.position.y / size.height).coerceIn(0f, 1f)
                                )
                            )
                        }
                    )
                }
                .testTag("draw_board")
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                if (points.size < 2) return@Canvas
                val path = Path()
                val first = points.first()
                path.moveTo(first.x * size.width, first.y * size.height)
                points.drop(1).forEach { point ->
                    path.lineTo(point.x * size.width, point.y * size.height)
                }
                drawPath(
                    path = path,
                    color = Color(colorArgb),
                    style = Stroke(
                        width = (widthFraction * size.width).coerceAtLeast(2f),
                        cap = StrokeCap.Round
                    )
                )
            }
            if (points.isEmpty()) {
                Text(
                    "Drag here to draw",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            DrawColors.forEach { argb ->
                val selected = colorArgb == argb
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(argb))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) colors.textPrimary else colors.borderSubtle,
                            shape = CircleShape
                        )
                        .clickable { colorArgb = argb }
                        .testTag("draw_color_$argb")
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Width", color = colors.textSecondary, fontSize = 11.sp)
            Slider(
                value = widthFraction,
                onValueChange = { widthFraction = it },
                valueRange = 0.004f..0.03f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
