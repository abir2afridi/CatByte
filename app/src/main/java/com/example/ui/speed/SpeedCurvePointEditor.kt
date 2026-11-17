package com.example.ui.speed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BezierPoint
import com.example.model.SpeedCurve
import com.example.ui.theme.PencilTheme
import java.util.Locale

/** Card editor for the currently selected curve control point. */
@Composable
internal fun SpeedCurvePointEditor(
    index: Int,
    point: BezierPoint,
    points: MutableList<BezierPoint>,
    speedCurve: SpeedCurve,
    onCurveChanged: (SpeedCurve) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = PencilTheme.colors

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
                IconButton(onClick = onDismiss) {
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
                Text("Speed: ${String.format(Locale.US, "%.2f", speed)}x", color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.width(100.dp))
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
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Point", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
