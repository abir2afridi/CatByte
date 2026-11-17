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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MediaClip

@Composable
fun AdjustmentsSheet(
    clip: MediaClip?,
    onUpdateAdjustments: (brightness: Float, contrast: Float, saturation: Float, vignette: Float) -> Unit,
    onClose: () -> Unit
) {
    var brightness by remember { mutableFloatStateOf(clip?.brightness ?: 0f) }
    var contrast by remember { mutableFloatStateOf(clip?.contrast ?: 1f) }
    var saturation by remember { mutableFloatStateOf(clip?.saturation ?: 1f) }
    var vignette by remember { mutableFloatStateOf(clip?.vignette ?: 0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF141416))
            .border(1.dp, Color(0xFF262832), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("adjustments_sheet")
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5A5B64))
            )
        }

        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎚️", fontSize = 16.sp)
                    Text("Adjustments & Shading", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var isComparing by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isComparing) Color(0xFF26E5D7) else Color(0xFF22242B))
                            .border(0.8.dp, Color(0xFF33353E), RoundedCornerShape(6.dp))
                            .clickable { isComparing = !isComparing }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isComparing) "Original" else "Before / After",
                            color = if (isComparing) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22242B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color(0xFF26E5D7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // HSL Color Channel Selection
            var selectedHslChannel by remember { mutableStateOf("All") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Red 🔴", "Orange 🟠", "Yellow 🟡", "Green 🟢", "Cyan 🔵", "Blue 🟣", "Magenta 🌸").forEach { channel ->
                    val isSel = channel == selectedHslChannel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFF26E5D7) else Color(0xFF22242B))
                            .border(0.8.dp, if (isSel) Color(0xFF26E5D7) else Color(0xFF33353E), RoundedCornerShape(6.dp))
                            .clickable { selectedHslChannel = channel }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = channel,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Brightness (Exposure)", color = Color(0xFFA5A8B2), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format(java.util.Locale.US, "%.2f", brightness), color = Color(0xFF26E5D7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                },
                valueRange = -0.5f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF26E5D7),
                    inactiveTrackColor = Color(0xFF22242B)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contrast (Intensity)", color = Color(0xFFA5A8B2), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format(java.util.Locale.US, "%.2f", contrast), color = Color(0xFF26E5D7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = contrast,
                onValueChange = {
                    contrast = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                },
                valueRange = 0.5f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF26E5D7),
                    inactiveTrackColor = Color(0xFF22242B)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Saturation (Vibrance)", color = Color(0xFFA5A8B2), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format(java.util.Locale.US, "%.2f", saturation), color = Color(0xFF26E5D7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                },
                valueRange = 0.0f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF26E5D7),
                    inactiveTrackColor = Color(0xFF22242B)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Vignette (Edge Shadow)", color = Color(0xFFA5A8B2), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format(java.util.Locale.US, "%.2f", vignette), color = Color(0xFF26E5D7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = vignette,
                onValueChange = {
                    vignette = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                },
                valueRange = 0.0f..1.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF26E5D7),
                    inactiveTrackColor = Color(0xFF22242B)
                )
            )
        }
    }
}
