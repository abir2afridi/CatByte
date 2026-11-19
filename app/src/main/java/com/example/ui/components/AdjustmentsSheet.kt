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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.model.HslAdjustment
import com.example.model.HslChannel
import com.example.model.MediaClip
import java.util.Locale

/**
 * Colour grading for one clip: exposure/contrast/saturation/vignette plus the
 * per-channel HSL grade.
 *
 * Every control here writes to the clip, and the shared composition applies
 * the same grade in the preview and in the export. The "Before / After" chip
 * is not decoration either: it asks the preview to show the clip without its
 * colour adjustments, so the user can compare against the untouched source.
 */
@Composable
fun AdjustmentsSheet(
    clip: MediaClip?,
    onUpdateAdjustments: (brightness: Float, contrast: Float, saturation: Float, vignette: Float) -> Unit,
    /** The full HSL list of the clip; the sheet edits one channel at a time. */
    onUpdateHsl: (List<HslAdjustment>) -> Unit = {},
    /** True while the preview shows the clip without its colour adjustments. */
    isComparing: Boolean = false,
    onCompareChanged: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    val clipId = clip?.id
    var brightness by remember(clipId) { mutableFloatStateOf(clip?.brightness ?: 0f) }
    var contrast by remember(clipId) { mutableFloatStateOf(clip?.contrast ?: 1f) }
    var saturation by remember(clipId) { mutableFloatStateOf(clip?.saturation ?: 1f) }
    var vignette by remember(clipId) { mutableFloatStateOf(clip?.vignette ?: 0f) }

    // The HSL grade is edited per colour channel; every channel owns its own
    // hue/saturation/luminance values.
    var selectedChannel by remember(clipId) { mutableStateOf(HslChannel.RED) }
    val hslAdjustments = clip?.hslAdjustments.orEmpty()
    val selectedAdjustment = hslAdjustments.firstOrNull { it.channel == selectedChannel }
        ?: HslAdjustment(selectedChannel)
    var hue by remember(clipId, selectedChannel) { mutableFloatStateOf(selectedAdjustment.hue) }
    var hslSaturation by remember(clipId, selectedChannel) {
        mutableFloatStateOf(selectedAdjustment.saturation)
    }
    var luminance by remember(clipId, selectedChannel) {
        mutableFloatStateOf(selectedAdjustment.luminance)
    }

    // While the user is not dragging, the committed clip value is authoritative.
    var hslEditing by remember(clipId, selectedChannel) { mutableStateOf(false) }
    LaunchedEffect(clipId, selectedChannel, selectedAdjustment) {
        if (!hslEditing) {
            hue = selectedAdjustment.hue
            hslSaturation = selectedAdjustment.saturation
            luminance = selectedAdjustment.luminance
        }
    }

    fun publishHsl(
        newHue: Float = hue,
        newSaturation: Float = hslSaturation,
        newLuminance: Float = luminance
    ) {
        val others = hslAdjustments.filter { it.channel != selectedChannel }
        val updated = HslAdjustment(
            channel = selectedChannel,
            hue = newHue,
            saturation = newSaturation,
            luminance = newLuminance
        )
        onUpdateHsl(others + updated)
    }

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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎚️", fontSize = 16.sp)
                    Text("Adjustments & Shading", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // The comparison is real: the preview drops the colour
                    // adjustments of this clip while the chip is active.
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isComparing) Color(0xFF26E5D7) else Color(0xFF22242B))
                            .border(0.8.dp, Color(0xFF33353E), RoundedCornerShape(6.dp))
                            .clickable { onCompareChanged(!isComparing) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("adjustments_compare")
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isComparing) {
                Text(
                    text = "Showing the clip without its colour adjustments. The project keeps the grade.",
                    color = Color(0xFF26E5D7),
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Colour channel selection: every chip edits a real channel of the
            // HSL grade (hue, saturation, luminance).
            Text(
                text = "Colour channels",
                color = Color(0xFFA5A8B2),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HslChannel.entries.forEach { channel ->
                    val isSel = channel == selectedChannel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) Color(0xFF26E5D7) else Color(0xFF22242B))
                            .border(0.8.dp, Color(0xFF33353E), RoundedCornerShape(6.dp))
                            .clickable { selectedChannel = channel }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("adjustments_hsl_${channel.name.lowercase()}")
                    ) {
                        Text(
                            text = channel.displayName,
                            color = if (isSel) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            GradeSlider(
                label = "Hue",
                valueText = String.format(Locale.US, "%.0f°", hue),
                value = hue,
                range = -180f..180f,
                tag = "adjustments_hsl_hue",
                onValueChange = {
                    hslEditing = true
                    hue = it
                    publishHsl(newHue = it)
                },
                onValueChangeFinished = { hslEditing = false }
            )
            GradeSlider(
                label = "Saturation",
                valueText = String.format(Locale.US, "%.0f", hslSaturation),
                value = hslSaturation,
                range = -100f..100f,
                tag = "adjustments_hsl_saturation",
                onValueChange = {
                    hslEditing = true
                    hslSaturation = it
                    publishHsl(newSaturation = it)
                },
                onValueChangeFinished = { hslEditing = false }
            )
            GradeSlider(
                label = "Luminance",
                valueText = String.format(Locale.US, "%.0f", luminance),
                value = luminance,
                range = -100f..100f,
                tag = "adjustments_hsl_luminance",
                onValueChange = {
                    hslEditing = true
                    luminance = it
                    publishHsl(newLuminance = it)
                },
                onValueChangeFinished = { hslEditing = false }
            )

            OutlinedButton(
                onClick = {
                    hslEditing = false
                    hue = 0f
                    hslSaturation = 0f
                    luminance = 0f
                    onUpdateHsl(hslAdjustments.filterNot { it.channel == selectedChannel })
                },
                enabled = !selectedAdjustment.isNeutral,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("adjustments_hsl_reset")
            ) {
                Text("Reset ${selectedChannel.displayName}", fontSize = 11.sp, maxLines = 1)
            }

            Spacer(modifier = Modifier.height(10.dp))

            GradeSlider(
                label = "Brightness",
                valueText = String.format(Locale.US, "%.2f", brightness),
                value = brightness,
                range = -0.5f..0.5f,
                tag = "adjustments_brightness",
                onValueChange = {
                    brightness = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                }
            )
            GradeSlider(
                label = "Contrast",
                valueText = String.format(Locale.US, "%.2f", contrast),
                value = contrast,
                range = 0.5f..1.5f,
                tag = "adjustments_contrast",
                onValueChange = {
                    contrast = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                }
            )
            GradeSlider(
                label = "Saturation",
                valueText = String.format(Locale.US, "%.2f", saturation),
                value = saturation,
                range = 0.0f..2.0f,
                tag = "adjustments_saturation",
                onValueChange = {
                    saturation = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                }
            )
            GradeSlider(
                label = "Vignette",
                valueText = String.format(Locale.US, "%.2f", vignette),
                value = vignette,
                range = 0.0f..1.0f,
                tag = "adjustments_vignette",
                onValueChange = {
                    vignette = it
                    onUpdateAdjustments(brightness, contrast, saturation, vignette)
                }
            )
        }
    }
}

/** One labelled grade slider with its value on the right. */
@Composable
private fun GradeSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFA5A8B2), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Text(valueText, color = Color(0xFF26E5D7), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished ?: {},
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF26E5D7),
                inactiveTrackColor = Color(0xFF22242B)
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}
