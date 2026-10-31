package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BezierPoint
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.ui.theme.PencilTheme

private const val CURVE_GRID_ROWS = 8
private const val CURVE_GRID_COLS = 10

// Top-level helper functions
fun buildCurvePath(points: List<BezierPoint>, width: Float, height: Float): Path {
    if (points.size < 2) return Path()

    val sortedPoints = points.sortedBy { it.x }
    val path = Path()

    val screenPoints = sortedPoints.map { p ->
        Offset(
            p.x * width,
            (1f - ((p.speedMultiplier.coerceIn(0.1f, 3f) - 0.1f) / 2.9f)) * height
        )
    }

    path.moveTo(screenPoints[0].x, screenPoints[0].y)

    if (sortedPoints.size == 2) {
        path.lineTo(screenPoints[1].x, screenPoints[1].y)
    } else {
        for (i in 0 until screenPoints.size - 1) {
            val p0 = if (i > 0) screenPoints[i - 1] else screenPoints[0]
            val p1 = screenPoints[i]
            val p2 = screenPoints[i + 1]
            val p3 = if (i + 2 < screenPoints.size) screenPoints[i + 2] else screenPoints.last()

            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }

    return path
}

fun generatePresetPoints(preset: SpeedCurvePreset): List<BezierPoint> = when (preset) {
    SpeedCurvePreset.STANDARD -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.MONTAGE -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.2f, 2.5f),
        BezierPoint(0.4f, 0.5f),
        BezierPoint(0.6f, 2.0f),
        BezierPoint(0.8f, 0.7f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.HERO_MOMENT -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.3f, 1.0f),
        BezierPoint(0.4f, 0.2f),
        BezierPoint(0.5f, 3.0f),
        BezierPoint(0.6f, 0.3f),
        BezierPoint(0.7f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.BULLET_TIME -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.25f, 1.0f),
        BezierPoint(0.35f, 0.15f),
        BezierPoint(0.65f, 0.15f),
        BezierPoint(0.75f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.JUMP_CUT -> listOf(
        BezierPoint(0.0f, 2.0f),
        BezierPoint(0.2f, 0.5f),
        BezierPoint(0.4f, 2.5f),
        BezierPoint(0.6f, 0.4f),
        BezierPoint(0.8f, 2.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.CUSTOM -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
}

@Composable
fun SpeedCurveEditor(
    speedCurve: SpeedCurve,
    onCurveChanged: (SpeedCurve) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val graphWidth = 300f
    val graphHeight = 200f

    var points by remember { mutableStateOf(speedCurve.points.toMutableList()) }
    var showPresets by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Preview curve path
    val path = remember(points) {
        buildCurvePath(points, graphWidth, graphHeight)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed Curve",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(24.dp))
                }
            }

            // Preset Selector
            if (showPresets) {
                PresetSelector(
                    currentPreset = speedCurve.preset,
                    onPresetSelected = { preset: SpeedCurvePreset ->
                        val newPoints = generatePresetPoints(preset)
                        points.clear()
                        points.addAll(newPoints)
                        onCurveChanged(speedCurve.copy(preset = preset, points = newPoints))
                        showPresets = false
                    },
                    onCustom = { showPresets = false }
                )
            }

            // Main Graph Area
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
                        val x = point.x * graphWidth
                        val normalizedSpeed = point.speedMultiplier.coerceIn(0.1f, 3f)
                        val speedRatio = (normalizedSpeed - 0.1f) / 2.9f
                        val y = (1f - speedRatio) * graphHeight

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

            // Selected Point Editor
            selectedPointIndex?.let { index ->
                val point = points[index]
                val normalizedSpeed = point.speedMultiplier.coerceIn(0.1f, 3f)
                val speedRatio = (normalizedSpeed - 0.1f) / 2.9f
                val y = (1f - speedRatio) * graphHeight

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Point ${index + 1}", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { selectedPointIndex = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(24.dp))
                            }
                        }

                        // Position X Slider
                        var posX by remember { mutableStateOf(point.x) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Position: ${(posX * 100).toInt()}%", color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                            Slider(
                                value = posX,
                                onValueChange = {
                                    posX = it.coerceIn(0f, 1f)
                                    points[index] = point.copy(x = posX)
                                    onCurveChanged(speedCurve.copy(points = points.toList()))
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accentCyan,
                                    activeTrackColor = colors.accentCyan,
                                    inactiveTrackColor = colors.borderSubtle
                                )
                            )
                        }

                        // Speed Slider
                        var speed by remember { mutableStateOf(point.speedMultiplier) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Speed: ${String.format("%.2f", speed)}x", color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.width(100.dp))
                            Slider(
                                value = speed.coerceIn(0.1f, 3f),
                                onValueChange = {
                                    speed = it.coerceIn(0.1f, 3f)
                                    points[index] = point.copy(speedMultiplier = speed)
                                    onCurveChanged(speedCurve.copy(points = points.toList()))
                                },
                                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = colors.accentCyan,
                                    activeTrackColor = colors.accentCyan,
                                    inactiveTrackColor = colors.borderSubtle
                                )
                            )
                        }

                        // Delete button for middle points
                        if (points.size > 2 && index != 0 && index != points.lastIndex) {
                            Button(
                                onClick = {
                                    points.removeAt(index)
                                    onCurveChanged(speedCurve.copy(points = points.toList()))
                                    selectedPointIndex = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Delete Point", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Control Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preset Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedCurvePreset.entries.forEach { preset ->
                        val isSelected = speedCurve.preset == preset
                        Button(
                            onClick = {
                                val newPoints = generatePresetPoints(preset)
                                points.clear()
                                points.addAll(newPoints)
                                onCurveChanged(speedCurve.copy(preset = preset, points = newPoints))
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.accentCyan else colors.surfaceElevated,
                                contentColor = if (isSelected) Color.White else colors.textPrimary
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(preset.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (points.size < 8) {
                                val last = points.last()
                                val newPoint = BezierPoint(
                                    x = (last.x + 0.15f).coerceAtMost(0.95f),
                                    speedMultiplier = last.speedMultiplier
                                )
                                points.add(points.size - 1, newPoint)
                                onCurveChanged(speedCurve.copy(points = points.toList()))
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Point", tint = colors.accentCyan, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Point", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (points.size > 2) {
                                points.removeAt(points.lastIndex - 1)
                                onCurveChanged(speedCurve.copy(points = points.toList()))
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Remove Point", tint = colors.accentRed, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text("Remove", color = colors.textPrimary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val resetPoints = generatePresetPoints(SpeedCurvePreset.STANDARD)
                            points.clear()
                            points.addAll(resetPoints)
                            onCurveChanged(speedCurve.copy(preset = SpeedCurvePreset.STANDARD, points = resetPoints))
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(18.dp))
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PresetSelector(
    currentPreset: SpeedCurvePreset,
    onPresetSelected: (SpeedCurvePreset) -> Unit,
    onCustom: () -> Unit
) {
    val colors = PencilTheme.colors

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speed Presets", color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                androidx.compose.material3.TextButton(onClick = onCustom) {
                    Text("Custom", color = colors.accentCyan, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpeedCurvePreset.entries.forEach { preset ->
                    val isSelected = currentPreset == preset
                    OutlinedButton(
                        onClick = { onPresetSelected(preset) },
                        modifier = Modifier.weight(1f).height(32.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) colors.accentCyan.copy(alpha = 0.15f) else colors.surface,
                            contentColor = if (isSelected) colors.accentCyan else colors.textPrimary,
                            disabledContainerColor = colors.surface,
                            disabledContentColor = colors.textTertiary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(preset.displayName, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                    }
                }
            }
        }
    }
}