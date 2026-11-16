package com.example.ui.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.ui.theme.PencilTheme

/**
 * Speed curve editor host: owns the working point list and preset visibility,
 * then composes the graph, the selected-point card and the action controls.
 */
@Composable
fun SpeedCurveEditor(
    speedCurve: SpeedCurve,
    onCurveChanged: (SpeedCurve) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors

    var points by remember { mutableStateOf(speedCurve.points.toMutableList()) }
    var showPresets by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

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
            SpeedCurveGraph(
                points = points,
                selectedPointIndex = selectedPointIndex
            )

            // Selected Point Editor
            selectedPointIndex?.let { index ->
                SpeedCurvePointEditor(
                    index = index,
                    point = points[index],
                    points = points,
                    speedCurve = speedCurve,
                    onCurveChanged = onCurveChanged,
                    onDismiss = { selectedPointIndex = null }
                )
            }

            // Control Bar
            SpeedCurveControls(
                speedCurve = speedCurve,
                points = points,
                onCurveChanged = onCurveChanged
            )
        }
    }
}
