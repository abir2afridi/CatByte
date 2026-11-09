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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.SpeedRampEngine
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.ui.theme.PencilTheme
import java.util.Locale

/**
 * CapCut-style Speed panel: duration line, 0.1x-100x presets, Smooth slow-mo
 * (needs GPU optical flow), Change audio pitch and the velocity-curve presets.
 *
 * Change audio pitch is real: it flips
 * [com.example.model.MediaClip.maintainPitch], which the render graph passes to
 * Media3's [androidx.media3.common.SpeedParameters].
 */
@Composable
fun SpeedSheet(
    currentSpeed: Float,
    clipDurationMs: Long = 0L,
    isChangePitch: Boolean = false,
    onSpeedSelected: (Float) -> Unit,
    onSelectSpeedCurve: ((SpeedCurve) -> Unit)? = null,
    onChangePitch: (Boolean) -> Unit = {},
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var speedVal by remember(currentSpeed) { mutableFloatStateOf(currentSpeed) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(318.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("speed_sheet")
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
                "Speed",
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Duration", color = colors.textSecondary, fontSize = 11.sp)
            Text(
                text = formatDuration(clipDurationMs),
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Slider(
            value = speedVal.coerceIn(0.1f, 100f),
            onValueChange = {
                speedVal = it
                onSpeedSelected(it)
            },
            valueRange = 0.1f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surfaceElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0.1x", color = colors.textTertiary, fontSize = 10.sp)
            Text(
                String.format(Locale.US, "%.2fx", speedVal),
                color = colors.accentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text("100x", color = colors.textTertiary, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.1f to null, 1f to null, 2f to null, 5f to "Pro", 10f to null, 100f to null)
                .forEach { (preset, badge) ->
                    val selected = kotlin.math.abs(speedVal - preset) < 0.01f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) colors.accentCyan.copy(alpha = 0.18f) else colors.surfaceElevated)
                            .border(
                                1.dp,
                                if (selected) colors.accentCyan else colors.borderSubtle,
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                speedVal = preset
                                onSpeedSelected(preset)
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("speed_preset_${preset.toString().replace('.', '_')}")
                    ) {
                        Text(
                            text = if (preset == 0.1f) "0.1x" else "${preset.toInt()}x",
                            color = if (selected) colors.accentCyan else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (badge != null) {
                            Text(badge, color = colors.accentYellow, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SpeedToggleRow(
            title = "Smooth slow-mo",
            subtitle = "Needs optical-flow interpolation (GPU)",
            checked = false,
            enabled = false,
            badge = "Pro",
            onCheckedChange = {}
        )
        SpeedToggleRow(
            title = "Change audio pitch",
            subtitle = "Turn off to keep the original voice pitch",
            checked = isChangePitch,
            enabled = true,
            onCheckedChange = onChangePitch
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Speed curve",
            color = colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpeedCurvePreset.entries.forEach { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                        .clickable {
                            val curve = SpeedRampEngine.getPresetCurve(preset)
                            onSelectSpeedCurve?.invoke(curve)
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("speed_curve_${preset.name.lowercase()}")
                ) {
                    Text(preset.displayName, color = colors.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SpeedToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    badge: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    title,
                    color = if (enabled) colors.textPrimary else colors.textTertiary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (badge != null) {
                    Text(
                        badge,
                        color = colors.accentYellow,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accentYellow.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(subtitle, color = colors.textTertiary, fontSize = 9.sp, maxLines = 1)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accentCyan,
                uncheckedThumbColor = colors.textTertiary,
                uncheckedTrackColor = colors.surfaceElevated
            ),
            modifier = Modifier.testTag("speed_toggle_${title.lowercase().replace(' ', '_')}")
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--.-"
    val totalTenths = (ms + 50L) / 100L
    val minutes = totalTenths / 600L
    val seconds = (totalTenths / 10L) % 60L
    val tenths = totalTenths % 10L
    return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
}
