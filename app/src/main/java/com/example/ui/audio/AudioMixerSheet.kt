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
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
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
import com.example.engine.AudioLoudness
import com.example.engine.AudioTiming
import com.example.model.AudioClip
import com.example.ui.components.PlaybackLevelBar
import com.example.ui.theme.PencilTheme
import com.example.viewmodel.features.TrackControlFeature
import java.util.Locale

private const val MIXER_MAX_SPEED = 16f
private val MIXER_SPEED_RANGE =
    AudioTiming.MIN_SPEED..AudioTiming.MAX_SPEED.coerceAtMost(MIXER_MAX_SPEED)

/**
 * Per-track audio mixer.
 *
 * Slider gestures keep a local draft while the finger is down and commit one
 * absolute value when the gesture ends. Preview and export consume the same
 * committed [AudioClip] model through the shared composition factory.
 */
@Composable
fun AudioMixerSheet(
    audioTracks: List<AudioClip>,
    selectedAudioId: String?,
    maxTimelineStartMs: Long,
    onSelectAudio: (String) -> Unit,
    onVolumeChanged: (String, Float) -> Unit,
    onNormaliseVolume: (String) -> Unit = {},
    onMuteChanged: (String, Boolean) -> Unit,
    onSoloChanged: (String, Boolean) -> Unit = { _, _ -> },
    onFadesChanged: (String, Long, Long) -> Unit,
    onSpeedChanged: (String, Float) -> Unit,
    onTrimChanged: (String, Long, Long) -> Unit,
    onTimelineStartChanged: (String, Long) -> Unit,
    onEffects: (String) -> Unit = {},
    onSplit: (String) -> Unit = {},
    onLoopCountChanged: (String, Int) -> Unit = { _, _ -> },
    onExportTrack: (String) -> Unit = {},
    onReset: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit,
    /** Live peak levels of the real preview output (0..1). */
    leftLevel: Float = 0f,
    rightLevel: Float = 0f
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // A soloed track is marked on its chip, so the mix
                        // state is visible without opening the track.
                        if (audio.isSoloed) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFFFFD600))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
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
        }

        if (selected != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MixerSectionTitle("Track: ${selected.title}")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live output meter: the bars follow the PCM leaving
                        // the preview sink, so they move with the mix instead
                        // of running a decorative animation.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.testTag("audio_mixer_output_meter")
                        ) {
                            Text(
                                "OUT",
                                color = colors.textSecondary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                PlaybackLevelBar(
                                    level = leftLevel,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(3.dp)
                                        .background(colors.surfaceElevated)
                                )
                                PlaybackLevelBar(
                                    level = rightLevel,
                                    color = Color(0xFF00E676),
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(3.dp)
                                        .background(colors.surfaceElevated)
                                )
                            }
                        }

                        // [S] Solo and [M] Mute buttons. Solo is real: the
                        // render graph silences every other audio track while
                        // this one is soloed.
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected.isSoloed) Color(0xFFFFD600) else colors.surfaceElevated)
                                .clickable { onSoloChanged(selected.id, !selected.isSoloed) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("audio_mixer_solo")
                        ) {
                            Text("S", color = if (selected.isSoloed) Color.Black else colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (selected.isMuted) Color(0xFFFF4D4D) else colors.surfaceElevated)
                                .clickable { onMuteChanged(selected.id, !selected.isMuted) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("audio_mixer_mute_button")
                        ) {
                            Text("M", color = if (selected.isMuted) Color.White else colors.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (selected.isSoloed) {
                    Text(
                        text = "Solo is on: every other audio track is silenced in the preview and in the export.",
                        color = Color(0xFFFFD600),
                        fontSize = 10.sp
                    )
                }

                MixerSlider(
                    stateKey = "volume:${selected.id}",
                    label = "Volume",
                    valueLabel = { "${(it * 100f).toInt()}%" },
                    value = selected.volume.coerceIn(0f, 2f),
                    range = 0f..2f,
                    tag = "audio_mixer_volume",
                    onValueChangeFinished = { onVolumeChanged(selected.id, it) }
                )

                // Loudness normalisation is real maths on the decoded
                // waveform; without data the button stays disabled and the
                // hint explains why instead of pretending.
                val canNormalise = AudioLoudness.canNormalise(selected.waveform)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (canNormalise) {
                            "Auto volume sets the gain to match target loudness"
                        } else {
                            "Auto volume unlocks once the waveform is decoded"
                        },
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onNormaliseVolume(selected.id) },
                        enabled = canNormalise,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("audio_mixer_auto_volume")
                    ) {
                        Text(
                            text = "Auto volume",
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

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
                    stateKey = "fade-in:${selected.id}",
                    label = "Fade in",
                    valueLabel = { "${it.toLong()} ms" },
                    value = selected.fadeInMs.coerceIn(0L, durationMs).toFloat(),
                    range = 0f..fadeInMax,
                    tag = "audio_mixer_fade_in",
                    onValueChangeFinished = {
                        onFadesChanged(selected.id, it.toLong(), selected.fadeOutMs)
                    }
                )
                MixerSlider(
                    stateKey = "fade-out:${selected.id}",
                    label = "Fade out",
                    valueLabel = { "${it.toLong()} ms" },
                    value = selected.fadeOutMs.coerceIn(0L, durationMs).toFloat(),
                    range = 0f..fadeOutMax,
                    tag = "audio_mixer_fade_out",
                    onValueChangeFinished = {
                        onFadesChanged(selected.id, selected.fadeInMs, it.toLong())
                    }
                )

                MixerSectionTitle("Speed")
                MixerSlider(
                    stateKey = "speed:${selected.id}",
                    label = "Playback speed",
                    valueLabel = { String.format(Locale.US, "%.2fx", it) },
                    value = selected.speed.coerceIn(
                        MIXER_SPEED_RANGE.start,
                        MIXER_SPEED_RANGE.endInclusive
                    ),
                    range = MIXER_SPEED_RANGE,
                    tag = "audio_mixer_speed",
                    onValueChangeFinished = { onSpeedChanged(selected.id, it) }
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
                    stateKey = "trim-in:${selected.id}",
                    label = "Source in",
                    valueLabel = { "${it.toLong()} ms" },
                    value = sourceStart.toFloat(),
                    range = 0f..(sourceDurationMs - AudioTiming.MIN_AUDIO_DURATION_MS)
                        .coerceAtLeast(0L).toFloat(),
                    tag = "audio_mixer_trim_in",
                    onValueChangeFinished = {
                        val end = if (selected.trimEndMs > 0L) selected.trimEndMs else sourceDurationMs
                        onTrimChanged(selected.id, it.toLong(), end)
                    }
                )
                MixerSlider(
                    stateKey = "trim-out:${selected.id}",
                    label = "Source out",
                    valueLabel = { "${it.toLong()} ms" },
                    value = sourceEnd.toFloat(),
                    range = (sourceStart + AudioTiming.MIN_AUDIO_DURATION_MS)
                        .coerceAtMost(sourceDurationMs).toFloat()..sourceDurationMs.toFloat(),
                    tag = "audio_mixer_trim_out",
                    onValueChangeFinished = {
                        onTrimChanged(selected.id, selected.trimStartMs, it.toLong())
                    }
                )

                MixerSectionTitle("Timeline placement")
                val maxStart = TrackControlFeature.maxAudioStartMs(
                    timelineDurationMs = maxTimelineStartMs,
                    clipDurationMs = selected.durationMs
                )
                MixerSlider(
                    stateKey = "timeline-start:${selected.id}",
                    label = "Timeline start",
                    valueLabel = { "${it.toLong()} ms" },
                    value = selected.startTimelineMs.coerceIn(0L, maxStart).toFloat(),
                    range = 0f..maxStart.toFloat(),
                    tag = "audio_mixer_start",
                    onValueChangeFinished = { onTimelineStartChanged(selected.id, it.toLong()) }
                )

                // Loop: the source window repeats, so a short music bed can
                // cover a long timeline without touching the source file.
                MixerSectionTitle("Loop")
                Text(
                    text = "Repeats the trimmed window ${selected.loopCount}x on the timeline",
                    color = colors.textSecondary,
                    fontSize = 10.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(1, 2, 4, 8).forEach { repeats ->
                        val active = selected.loopCount == repeats
                        OutlinedButton(
                            onClick = { onLoopCountChanged(selected.id, repeats) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("audio_mixer_loop_$repeats")
                        ) {
                            Text(
                                text = "${repeats}x",
                                fontSize = 10.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) colors.accentCyan else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEffects(selected.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("audio_mixer_effects")
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Effects", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onSplit(selected.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("audio_mixer_split")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Split at playhead", fontSize = 11.sp)
                    }
                }

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

                // One-track export: the file holds this track only, so the
                // result can be handed off without the rest of the timeline.
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onExportTrack(selected.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("audio_mixer_export_track")
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export this track (.m4a)", fontSize = 11.sp)
                }
                Text(
                    text = "Writes this track on its own: the video and the other tracks stay out of the file.",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                )
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
    stateKey: String,
    label: String,
    valueLabel: (Float) -> String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChangeFinished: (Float) -> Unit
) {
    val colors = PencilTheme.colors
    val clampedExternalValue = value.coerceIn(range.start, range.endInclusive)
    var draftValue by remember(stateKey) { mutableFloatStateOf(clampedExternalValue) }
    var isEditing by remember(stateKey) { mutableStateOf(false) }

    // A committed model value is authoritative whenever the slider is idle.
    // During a gesture the draft wins, so a parent recomposition cannot snap the
    // thumb back to the pre-edit value before onValueChangeFinished commits it.
    LaunchedEffect(stateKey, clampedExternalValue, range, isEditing) {
        if (!isEditing) {
            draftValue = clampedExternalValue
        } else {
            draftValue = draftValue.coerceIn(range.start, range.endInclusive)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textSecondary, fontSize = 11.sp, modifier = Modifier.width(86.dp))
        Slider(
            value = draftValue.coerceIn(range.start, range.endInclusive),
            onValueChange = {
                isEditing = true
                draftValue = it.coerceIn(range.start, range.endInclusive)
            },
            onValueChangeFinished = {
                val committedValue = draftValue.coerceIn(range.start, range.endInclusive)
                isEditing = false
                onValueChangeFinished(committedValue)
            },
            valueRange = range,
            modifier = Modifier
                .weight(1f)
                .testTag(tag)
        )
        Text(
            text = valueLabel(draftValue),
            color = colors.textPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(62.dp)
        )
    }
}
