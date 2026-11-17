package com.example.ui.audio

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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioEffectSettings
import com.example.model.VoiceEffectType
import com.example.ui.theme.PencilTheme
import kotlin.math.roundToInt

/**
 * The "Audio effects" sheet for one clip or independent audio track.
 *
 * The controls edit a local draft; every committed change flows straight into
 * the [AudioEffectSettings] on the model, which the shared composition factory
 * turns into the same offline DSP chain for preview and export. Nothing here
 * needs a cloud model: the presets are real kernels in
 * [com.example.engine.AudioDsp].
 */
@Composable
fun AudioEffectsSheet(
    targetTitle: String,
    settings: AudioEffectSettings,
    onSettingsChange: (AudioEffectSettings) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    var draft by remember(settings) { mutableStateOf(settings) }
    var intensityDraft by remember(draft.type) {
        mutableFloatStateOf(draft.intensity.coerceIn(0f, 1f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 420.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("audio_effects_sheet")
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
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = "Audio effects",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = targetTitle,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(30.dp)
                    .testTag("audio_effects_done")
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Voice effect",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                VoiceEffectType.entries.forEach { preset ->
                    val active = draft.type == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) colors.accentCyan.copy(alpha = 0.2f)
                                else colors.surfaceElevated
                            )
                            .border(
                                1.dp,
                                if (active) colors.accentCyan else colors.borderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                val next = draft.copy(intensity = intensityDraft)
                                draft = next.copy(type = preset)
                                onSettingsChange(draft)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("audio_effect_chip_${preset.name}")
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (active) colors.accentCyan else colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            if (draft.type.usesKernel) {
                Text(
                    text = "Intensity",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = intensityDraft,
                        onValueChange = { intensityDraft = it.coerceIn(0f, 1f) },
                        onValueChangeFinished = {
                            draft = draft.copy(intensity = intensityDraft)
                            onSettingsChange(draft)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_effect_intensity")
                    )
                    Text(
                        text = "${(intensityDraft * 100).roundToInt()}%",
                        color = colors.textPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Text(
                text = "Clean-up",
                color = colors.accentCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)
            )
            ToggleRow(
                label = "Enhance voice",
                description = "Rumble cut + presence shelf for speech clarity",
                checked = draft.enhanceVoice,
                tag = "audio_effect_enhance_voice"
            ) { checked ->
                draft = draft.copy(enhanceVoice = checked)
                onSettingsChange(draft)
            }
            ToggleRow(
                label = "Reduce noise",
                description = "Gate the quiet hiss between words",
                checked = draft.reduceNoise,
                tag = "audio_effect_reduce_noise"
            ) { checked ->
                draft = draft.copy(reduceNoise = checked)
                onSettingsChange(draft)
            }
            ToggleRow(
                label = "Isolate voice",
                description = "Keep the speech band (180 Hz - 5 kHz), drop the rest",
                checked = draft.isolateVoice,
                tag = "audio_effect_isolate_voice"
            ) { checked ->
                draft = draft.copy(isolateVoice = checked)
                onSettingsChange(draft)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Runs offline on this device; preview and export use the same processing.",
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Honest empty state: the effects sheet can only edit a selected video clip
 * or audio track, so without one it says so instead of pretending.
 */
@Composable
fun AudioEffectsSheetEmpty(onClose: () -> Unit) {
    val colors = PencilTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("audio_effects_sheet")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio effects",
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select a clip or audio track first",
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Voice presets, enhance voice and reduce noise work on the selected clip.",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 12.sp
            )
            Text(
                text = description,
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
