package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToInt

/**
 * CapCut-style Volume panel: title + check, a 0-1000% gain slider, the clip
 * audio fades and then the Reduce noise and Loudness adjustment switches.
 *
 * Loudness adjustment is a real, waveform-driven normalisation
 * ([com.example.engine.AudioLoudness]); reduce noise runs the on-device
 * peak gate from [com.example.engine.AudioDsp]; the fades are a real gain
 * envelope in the shared composition, so preview and export match.
 */
@Composable
fun VolumeSheet(
    currentVolume: Float,
    isNoiseReductionEnabled: Boolean = false,
    isLoudnessAdjusted: Boolean = false,
    canNormaliseLoudness: Boolean = false,
    /** The clip's own audio can be gated when the on-device DSP is available. */
    canReduceNoise: Boolean = false,
    /** Fade in/out of the clip's own audio, in output-timeline milliseconds. */
    audioFadeInMs: Long = 0L,
    audioFadeOutMs: Long = 0L,
    /** Longest fade the clip can take (its output duration). */
    maxFadeMs: Long = 0L,
    onVolumeChange: (Float) -> Unit,
    onToggleNoiseReduction: (Boolean) -> Unit = {},
    onToggleLoudnessAdjustment: (Boolean) -> Unit = {},
    onAudioFadesChange: (Long, Long) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var volume by remember(currentVolume) { mutableFloatStateOf(currentVolume) }
    var fadeIn by remember(audioFadeInMs) { mutableStateOf(audioFadeInMs) }
    var fadeOut by remember(audioFadeOutMs) { mutableStateOf(audioFadeOutMs) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 252.dp, max = 430.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(1.dp, colors.borderSubtle.copy(alpha = 0.4f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("volume_sheet")
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
                "Volume",
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

        Spacer(modifier = Modifier.height(2.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
        Text(
            text = "${(volume * 100).roundToInt()}",
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Slider(
            value = volume,
            onValueChange = {
                volume = it
                onVolumeChange(it)
            },
            valueRange = 0f..10f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surfaceElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .testTag("volume_slider")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("0", color = colors.textTertiary, fontSize = 10.sp)
            Text("1000", color = colors.textTertiary, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (maxFadeMs > 0L) {
            Text(
                text = "Clip audio fades",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            FadeSlider(
                label = "Fade in",
                valueMs = fadeIn,
                maxMs = maxFadeMs,
                tag = "volume_fade_in",
                onValueChangeFinished = {
                    fadeIn = it
                    onAudioFadesChange(it, fadeOut)
                }
            )
            FadeSlider(
                label = "Fade out",
                valueMs = fadeOut,
                maxMs = maxFadeMs,
                tag = "volume_fade_out",
                onValueChangeFinished = {
                    fadeOut = it
                    onAudioFadesChange(fadeIn, it)
                }
            )
        }

        VolumeToggleRow(
            title = "Reduce noise",
            subtitle = if (canReduceNoise) {
                "Gates the quiet hiss between words (on-device)"
            } else {
                "Needs a denoise model — a chat/image endpoint cannot do this"
            },
            checked = isNoiseReductionEnabled,
            enabled = canReduceNoise,
            badge = if (canReduceNoise) null else "Model",
            onCheckedChange = onToggleNoiseReduction
        )
        VolumeToggleRow(
            title = "Loudness adjustment",
            subtitle = if (canNormaliseLoudness) {
                "Normalises this clip to a steady listening level"
            } else {
                "Analysing the clip audio…"
            },
            checked = isLoudnessAdjusted,
            enabled = canNormaliseLoudness,
            onCheckedChange = onToggleLoudnessAdjustment
        )
        }
    }
}

/** One fade slider with its millisecond readout. */
@Composable
private fun FadeSlider(
    label: String,
    valueMs: Long,
    maxMs: Long,
    tag: String,
    onValueChangeFinished: (Long) -> Unit
) {
    val colors = PencilTheme.colors
    var draft by remember(valueMs) { mutableStateOf(valueMs.toFloat()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(58.dp)
        )
        Slider(
            value = draft.coerceIn(0f, maxMs.toFloat()),
            onValueChange = { draft = it.coerceIn(0f, maxMs.toFloat()) },
            onValueChangeFinished = { onValueChangeFinished(draft.toLong()) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surfaceElevated
            ),
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
        )
        Text(
            text = "${draft.toLong()} ms",
            color = colors.textPrimary,
            fontSize = 10.sp,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun VolumeToggleRow(
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
            .padding(horizontal = 16.dp, vertical = 7.dp),
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
            modifier = Modifier.testTag("volume_toggle_${title.lowercase().replace(' ', '_')}")
        )
    }
}
