package com.example.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.GainProcessor
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.data.export.AudioEffectProcessors
import com.example.data.export.ConstantAudioSpeedProvider
import com.example.data.export.ConstantGainProvider
import com.example.data.export.DuckGainProvider
import com.example.engine.AudioSpec
import com.example.engine.AudioTiming
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
    val gain = spec.volume.coerceIn(0f, 2f)
    // A looping track repeats the same source window, so every repeat is one
    // item of the sequence. The track covers loopCount times the window.
    val repeats = spec.loopCount.coerceIn(1, AudioTiming.MAX_LOOP_COUNT)
    val unitDurationMs = if (repeats > 1) {
        (((spec.sourceOutUs - spec.sourceInUs) / 1_000L).toDouble() / spec.speed.toDouble())
            .toLong()
            .coerceAtLeast(1L)
    } else {
        ((spec.timelineOutUs - spec.timelineInUs) / 1_000L).coerceAtLeast(1L)
    }
    // The fades belong to the whole track: the first repeat fades in, the last
    // one fades out, and the repeats in between play at full level.
    val fadeInMs = (spec.fadeInUs / 1_000L).coerceIn(0L, unitDurationMs)
    val fadeOutMs = (spec.fadeOutUs / 1_000L).coerceIn(0L, unitDurationMs)
    // Per-track DSP chain (gate -> voice EQ -> creative preset -> pitch) runs
    // before the gain envelope, so the mixer's volume slider stays a pure
    // level control. No-op settings produce no processors at all.
    val effectProcessors = AudioEffectProcessors.forSettings(audio.audioEffects)

    val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
    if (spec.timelineInUs > 0L) {
        builder.addGap(spec.timelineInUs)
    }

    for (repeat in 0 until repeats) {
        val isFirst = repeat == 0
        val isLast = repeat == repeats - 1
        val baseGain = ConstantGainProvider(
            gain = gain,
            fadeInMs = if (isFirst) fadeInMs else 0L,
            fadeOutMs = if (isLast) fadeOutMs else 0L,
            durationMs = unitDurationMs
        )
        // Auto-ducking: only music ducks, a voice-over never ducks itself.
        // Each repeat knows its own timeline offset, so the duck follows the
        // voice-over across the whole looping track.
        val gainProvider = if (
            !audio.isVoiceover && voiceRanges.isNotEmpty() && gain > 0f
        ) {
            DuckGainProvider(
                base = baseGain,
                startTimelineMs = (spec.timelineInUs / 1_000L) + repeat * unitDurationMs,
                voiceRanges = voiceRanges
            )
        } else {
            baseGain
        }
        val processors = effectProcessors + GainProcessor(gainProvider)
        val itemBuilder = EditedMediaItem.Builder(mediaItem)
            // Media3 requires the ORIGINAL source duration here. Clipping and
            // speed are applied afterwards to derive the presentation duration.
            .setDurationUs(spec.sourceDurationUs.coerceAtLeast(1_000L))
            .setRemoveVideo(true)
            .setEffects(Effects(processors, emptyList()))
        if (abs(spec.speed - 1f) > 0.001f && spec.speed > 0.01f) {
            val clippedSourceDurationUs = (spec.sourceOutUs - spec.sourceInUs).coerceAtLeast(1L)
            itemBuilder.setSpeed(
                SpeedParameters(
                    ConstantAudioSpeedProvider(
                        requestedSpeed = spec.speed,
                        clippedSourceDurationUs = clippedSourceDurationUs,
                        targetOutputDurationUs = unitDurationMs * 1_000L
                    ),
                    true
                )
            )
        }
        builder.addItem(itemBuilder.build())
    }
    return builder.build()
}
