package com.example.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.GainProcessor
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.data.export.ConstantAudioSpeedProvider
import com.example.data.export.ConstantGainProvider
import com.example.data.export.DuckGainProvider
import com.example.engine.AudioSpec
import com.example.model.AudioClip
import java.io.File
import kotlin.math.abs

/** Builds the independent audio-track sequence, including its timeline gap. */
internal fun ProjectCompositionFactory.buildAudioSequence(
    audio: AudioClip,
    spec: AudioSpec,
    /** Timeline spans where a voice-over is audible; music ducks underneath. */
    voiceRanges: List<LongRange> = emptyList()
): EditedMediaItemSequence {
    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
    val sourceInMs = spec.sourceInUs / 1_000L
    val sourceOutMs = spec.sourceOutUs / 1_000L
    if (sourceInMs > 0L) {
        clippingBuilder.setStartPositionMs(sourceInMs)
    }
    if (sourceOutMs > sourceInMs) {
        clippingBuilder.setEndPositionMs(sourceOutMs)
    }

    val uri = audio.uriString?.let { value ->
        if (value.startsWith("/")) Uri.fromFile(File(value)) else Uri.parse(value)
    } ?: Uri.EMPTY
    val mediaItem = MediaItem.Builder()
        .setUri(uri)
        .setClippingConfiguration(clippingBuilder.build())
        .build()
    val outputDurationMs = ((spec.timelineOutUs - spec.timelineInUs) / 1_000L)
        .coerceAtLeast(1L)
    val gain = spec.volume.coerceIn(0f, 2f)
    val baseGain = ConstantGainProvider(
        gain = gain,
        fadeInMs = spec.fadeInUs / 1_000L,
        fadeOutMs = spec.fadeOutUs / 1_000L,
        durationMs = outputDurationMs
    )
    // Auto-ducking: only music ducks, a voice-over never ducks itself.
    val gainProvider = if (!audio.isVoiceover && voiceRanges.isNotEmpty() && gain > 0f) {
        DuckGainProvider(
            base = baseGain,
            startTimelineMs = spec.timelineInUs / 1_000L,
            voiceRanges = voiceRanges
        )
    } else {
        baseGain
    }
    val processor = GainProcessor(gainProvider)
    val itemBuilder = EditedMediaItem.Builder(mediaItem)
        // Media3 requires the ORIGINAL source duration here. Clipping and
        // speed are applied afterwards to derive the presentation duration.
        .setDurationUs(spec.sourceDurationUs.coerceAtLeast(1_000L))
        .setRemoveVideo(true)
        .setEffects(Effects(listOf(processor), emptyList()))
    if (abs(spec.speed - 1f) > 0.001f && spec.speed > 0.01f) {
        val clippedSourceDurationUs = (spec.sourceOutUs - spec.sourceInUs).coerceAtLeast(1L)
        itemBuilder.setSpeed(
            SpeedParameters(
                ConstantAudioSpeedProvider(
                    requestedSpeed = spec.speed,
                    clippedSourceDurationUs = clippedSourceDurationUs,
                    targetOutputDurationUs = outputDurationMs * 1_000L
                ),
                true
            )
        )
    }
    val item = itemBuilder.build()

    val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
    if (spec.timelineInUs > 0L) {
        builder.addGap(spec.timelineInUs)
    }
    builder.addItem(item)
    return builder.build()
}
