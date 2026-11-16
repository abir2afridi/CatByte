package com.example.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.SpeedParameters
import androidx.media3.common.audio.GainProcessor
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.data.export.ConstantGainProvider
import com.example.data.export.ConstantSpeedProvider
import com.example.model.AudioClip
import kotlin.math.abs

/** Builds the independent audio-track sequence, including its timeline gap. */
internal fun ProjectCompositionFactory.buildAudioSequence(
    audio: AudioClip,
    startTimelineUs: Long
): EditedMediaItemSequence {
    val clippingBuilder = MediaItem.ClippingConfiguration.Builder()
    if (audio.trimStartMs > 0L || audio.trimEndMs > audio.trimStartMs) {
        clippingBuilder.setStartPositionMs(audio.trimStartMs.coerceAtLeast(0L))
        if (audio.trimEndMs > audio.trimStartMs) {
            clippingBuilder.setEndPositionMs(audio.trimEndMs)
        }
    }
    val mediaItem = MediaItem.Builder()
        .setUri(Uri.parse(audio.uriString))
        .setClippingConfiguration(clippingBuilder.build())
        .build()
    val gain = if (audio.isMuted) 0f else audio.volume.coerceIn(0f, 2f)
    val processor = GainProcessor(
        ConstantGainProvider(
            gain = gain,
            fadeInMs = audio.fadeInMs,
            fadeOutMs = audio.fadeOutMs,
            durationMs = audio.durationMs
        )
    )
    val itemBuilder = EditedMediaItem.Builder(mediaItem)
        .setDurationUs(audio.durationMs.coerceAtLeast(1L) * 1000L)
        .setRemoveVideo(true)
        .setEffects(Effects(listOf(processor), emptyList()))
    if (abs(audio.speed - 1f) > 0.001f && audio.speed > 0.01f) {
        itemBuilder.setSpeed(
            SpeedParameters(ConstantSpeedProvider(audio.speed.coerceIn(0.1f, 16f)), true)
        )
    }
    val item = itemBuilder.build()

    val builder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
    if (startTimelineUs > 0L) {
        builder.addGap(startTimelineUs)
    }
    builder.addItem(item)
    return builder.build()
}
