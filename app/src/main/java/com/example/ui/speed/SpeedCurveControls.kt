package com.example.ui.speed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BezierPoint
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.ui.theme.PencilTheme

/** Preset picker row plus add / remove / reset actions. */
@Composable
internal fun SpeedCurveControls(
    speedCurve: SpeedCurve,
    points: MutableList<BezierPoint>,
    onCurveChanged: (SpeedCurve) -> Unit
) {
    val colors = PencilTheme.colors

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
                    colors = ButtonDefaults.buttonColors(
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Point", tint = colors.accentCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.surfaceElevated),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Remove Point", tint = colors.accentRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
