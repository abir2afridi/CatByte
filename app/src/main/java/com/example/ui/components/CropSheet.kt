package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CropMath
import com.example.ui.theme.PencilTheme

/**
 * Crop panel. Cropping is a real centre crop: the chosen preset computes a
 * normalised [CropMath] rectangle from the source aspect and writes it to the
 * clip's scale/position transform, which both preview and export read.
 */
@Composable
fun CropSheet(
    sourceWidth: Int,
    sourceHeight: Int,
    currentScale: Float,
    currentPosX: Float,
    currentPosY: Float,
    onApply: (scale: Float, posX: Float, posY: Float) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var scale by remember(currentScale) { mutableFloatStateOf(currentScale) }
    var posX by remember(currentPosX) { mutableFloatStateOf(currentPosX) }
    var posY by remember(currentPosY) { mutableFloatStateOf(currentPosY) }

    val sourceAspect = if (sourceWidth > 0 && sourceHeight > 0) {
        sourceWidth.toFloat() / sourceHeight.toFloat()
    } else {
        16f / 9f
    }

    fun applyPreset(label: String) {
        val target = when (label) {
            "1:1" -> 1f
            "9:16" -> 9f / 16f
            "16:9" -> 16f / 9f
            "4:5" -> 4f / 5f
            else -> sourceAspect
        }
        val crop = if (label == "Original") {
            CropMath.FULL
        } else {
            CropMath.byAspect(target / sourceAspect)
        }
        scale = (1f / minOf(crop.width, crop.height).coerceAtLeast(0.02f)).coerceIn(1f, 5f)
        posX = 0.5f
        posY = 0.5f
        onApply(scale, posX, posY)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("crop_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.textTertiary.copy(alpha = 0.4f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Crop",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        scale = 1f
                        posX = 0.5f
                        posY = 0.5f
                        onReset()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset crop", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Check, contentDescription = "Close", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Original", "1:1", "9:16", "16:9", "4:5").forEach { label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable { applyPreset(label) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("crop_${label.replace(":", "_")}")
                ) {
                    Text(label, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Zoom", color = colors.textSecondary, fontSize = 11.sp)
                Text(String.format("%.2fx", scale), color = colors.accentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = scale,
                onValueChange = {
                    scale = it
                    onApply(scale, posX, posY)
                },
                valueRange = 1f..5f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentCyan,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Position X", color = colors.textSecondary, fontSize = 11.sp)
                Text("${(posX * 100).toInt()}%", color = colors.textTertiary, fontSize = 10.sp)
            }
            Slider(
                value = posX,
                onValueChange = {
                    posX = it
                    onApply(scale, posX, posY)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.textPrimary,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Position Y", color = colors.textSecondary, fontSize = 11.sp)
                Text("${(posY * 100).toInt()}%", color = colors.textTertiary, fontSize = 10.sp)
            }
            Slider(
                value = posY,
                onValueChange = {
                    posY = it
                    onApply(scale, posX, posY)
                },
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.textPrimary,
                    activeTrackColor = colors.accentCyan,
                    inactiveTrackColor = colors.surfaceElevated
                )
            )
        }
    }
}
