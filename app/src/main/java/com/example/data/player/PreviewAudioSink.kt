package com.example.data.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Wires the live meter into the preview audio sink.
 *
 * Both preview paths use it: the [androidx.media3.transformer.CompositionPlayer]
 * takes the sink directly, and the fallback ExoPlayer gets it through
 * [DefaultRenderersFactory.buildAudioSink]. The tap therefore measures real
 * playback on either path instead of an animation.
 */

/** The sink whose audio-processor chain includes the meter tap. */
@UnstableApi
internal fun buildMeteredAudioSink(context: Context): AudioSink =
    DefaultAudioSink.Builder(context)
        .setAudioProcessors(arrayOf(PlaybackAudioMeter.PeakProcessor()))
        .build()

/** Renderers factory for the ExoPlayer fallback, using the metered sink. */
@UnstableApi
internal fun meteredRenderersFactory(context: Context): DefaultRenderersFactory =
    object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink = buildMeteredAudioSink(context)
    }
