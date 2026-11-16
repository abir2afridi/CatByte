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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.ui.theme.PencilTheme
import java.util.Locale

/**
 * Per-track audio mixer.
 *
 * The controls write directly to the selected [AudioClip]. Preview and export
 * both consume that same model through the shared composition factory.
 */
@Composable
fun AudioMixerSheet(
    audioTracks: List<AudioClip>,
    selectedAudioId: String?,
    maxTimelineStartMs: Long,
    onSelectAudio: (String) -> Unit,
    onVolumeChanged: (String, Float) -> Unit,
    onMuteChanged: (String, Boolean) -> Unit,
    onFadesChanged: (String, Long, Long) -> Unit,
    onSpeedChanged: (String, Float) -> Unit,
    onTrimChanged: (String, Long, Long) -> Unit,
    onTimelineStartChanged: (String, Long) -> Unit,
    onReset: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val colors = PencilTheme.colors
    val selected = audioTracks.firstOrNull { it.id == selectedAudioId }
        ?: audioTracks.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp, max = 440.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .border(
                1.dp,
                colors.borderSubtle.copy(alpha = 0.4f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .testTag("audio_mixer_sheet")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Audio mixer",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (audioTracks.isEmpty()) "No audio tracks" else "${audioTracks.size} track(s)",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close mixer",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }

        if (audioTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Import or record an audio track first",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The mixer changes volume, fades, speed, trim and placement per track.",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            audioTracks.forEach { audio ->
                val active = audio.id == selected?.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) colors.accentCyan.copy(alpha = 0.2f) else colors.surfaceElevated)
                        .border(
                            1.dp,
                            if (active) colors.accentCyan else colors.borderSubtle,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectAudio(audio.id) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("audio_mixer_track_${audio.id}")
                ) {
                    Text(
                        text = audio.title,
                        color = if (active) colors.accentCyan else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        if (selected != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                MixerSectionTitle("Track: ${selected.title}")

                MixerSlider(
                    label = "Volume",
                    valueLabel = "${(selected.volume * 100f).toInt()}%",
                    value = selected.volume.coerceIn(0f, 2f),
                    range = 0f..2f,
                    tag = "audio_mixer_volume",
                    onValueChange = { onVolumeChanged(selected.id, it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mute", color = colors.textSecondary, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (selected.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (selected.isMuted) colors.accentRed else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = selected.isMuted,
                            onCheckedChange = { onMuteChanged(selected.id, it) },
                            modifier = Modifier.testTag("audio_mixer_mute")
                        )
                    }
                }

                val durationMs = AudioTiming.presentationDurationMs(selected)
                val fadeInMax = (durationMs - selected.fadeOutMs).coerceAtLeast(0L).toFloat()
                val fadeOutMax = (durationMs - selected.fadeInMs).coerceAtLeast(0L).toFloat()
                MixerSectionTitle("Fades (timeline)")
                MixerSlider(
                    label = "Fade in",
                    valueLabel = "${selected.fadeInMs} ms",
                    value = selected.fadeInMs.coerceIn(0L, durationMs).toFloat(),
                    range = 0f..fadeInMax.coerceAtLeast(1f),
                    tag = "audio_mixer_fade_in",
                    onValueChange = {
                        onFadesChanged(selected.id, it.toLong(), selected.fadeOutMs)
                    }
                )
                MixerSlider(
                    label = "Fade out",
                    valueLabel = "${selected.fadeOutMs} ms",
                    value = selected.fadeOutMs.coerceIn(0L, durationMs).toFloat(),
                    range = 0f..fadeOutMax.coerceAtLeast(1f),
                    tag = "audio_mixer_fade_out",
                    onValueChange = {
                        onFadesChanged(selected.id, selected.fadeInMs, it.toLong())
                    }
                )

                MixerSectionTitle("Speed")
                MixerSlider(
                    label = "Playback speed",
                    valueLabel = String.format(Locale.US, "%.2fx", selected.speed),
                    value = selected.speed.coerceIn(0.25f, 3f),
                    range = 0.25f..3f,
                    tag = "audio_mixer_speed",
                    onValueChange = { onSpeedChanged(selected.id, it) }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(0.5f, 0.75f, 1f, 1.5f, 2f).forEach { speed ->
                        OutlinedButton(
                            onClick = { onSpeedChanged(selected.id, speed) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("audio_mixer_speed_${speed}")
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2gx", speed),
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                val sourceDurationMs = AudioTiming.sourceDurationMs(selected)
                val sourceStart = AudioTiming.sourceStartMs(selected)
                val sourceEnd = AudioTiming.sourceEndMs(selected)
                MixerSectionTitle("Source trim")
                Text(
                    text = "Source in: ${sourceStart} ms · Source out: ${sourceEnd} ms",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
                MixerSlider(
                    label = "Source in",
                    valueLabel = "$sourceStart ms",
                    value = sourceStart.toFloat(),
                    range = 0f..(sourceDurationMs - AudioTiming.MIN_AUDIO_DURATION_MS)
                        .coerceAtLeast(0L).toFloat(),
                    tag = "audio_mixer_trim_in",
                    onValueChange = {
                        val end = if (selected.trimEndMs > 0L) selected.trimEndMs else sourceDurationMs
                        onTrimChanged(selected.id, it.toLong(), end)
                    }
                )
                MixerSlider(
                    label = "Source out",
                    valueLabel = "$sourceEnd ms",
                    value = sourceEnd.toFloat(),
                    range = (sourceStart + AudioTiming.MIN_AUDIO_DURATION_MS)
                        .coerceAtMost(sourceDurationMs).toFloat()..sourceDurationMs.toFloat(),
                    tag = "audio_mixer_trim_out",
                    onValueChange = {
                        onTrimChanged(selected.id, selected.trimStartMs, it.toLong())
                    }
                )

                MixerSectionTitle("Timeline placement")
                val maxStart = maxTimelineStartMs.coerceAtLeast(0L)
                MixerSlider(
                    label = "Timeline start",
                    valueLabel = "${selected.startTimelineMs} ms",
                    value = selected.startTimelineMs.coerceIn(0L, maxStart).toFloat(),
                    range = 0f..maxStart.coerceAtLeast(1L).toFloat(),
                    tag = "audio_mixer_start",
                    onValueChange = { onTimelineStartChanged(selected.id, it.toLong()) }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onReset(selected.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("audio_mixer_reset")
                    ) {
                        Text("Reset", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onDelete(selected.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accentRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("audio_mixer_delete")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MixerSectionTitle(text: String) {
    val colors = PencilTheme.colors
    Text(
        text = text,
        color = colors.accentCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun MixerSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChange: (Float) -> Unit
) {
    val colors = PencilTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.width(86.dp))
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
        )
        Text(
            text = valueLabel,
            color = colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(62.dp)
        )
    }
}
