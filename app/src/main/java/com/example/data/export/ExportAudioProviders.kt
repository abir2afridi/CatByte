package com.example.data.export

import androidx.media3.common.C
import androidx.media3.common.audio.GainProcessor
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.common.util.UnstableApi
import com.example.engine.AudioMath

/**
 * Audio providers shared by the preview mixer and the exported render graph.
 *
 * They live in their own file so the gain/speed maths can be reused (and
 * fixed) without touching the export pipeline.
 */

/** A constant playback speed for a clip. */
@UnstableApi
class ConstantSpeedProvider(private val speed: Float) : SpeedProvider {
    override fun getSpeed(presentationTimeUs: Long): Float = speed
    override fun getNextSpeedChangeTimeUs(presentationTimeUs: Long): Long = C.TIME_END_OF_SOURCE
}

/** Applies a constant volume with optional fade in / fade out. */
@UnstableApi
class ConstantGainProvider(
    private val gain: Float,
    private val fadeInMs: Long,
    private val fadeOutMs: Long,
    private val durationMs: Long
) : GainProcessor.GainProvider {

    override fun getGainFactorAtSamplePosition(presentationTimeUs: Long, sampleRate: Int): Float {
        val base = gain.coerceIn(0f, 2f)
        if (durationMs <= 0L) return base
        // Shared with the preview mixer (AudioMath) so fades match exactly.
        return base * AudioMath.fadeGain(
            positionMs = presentationTimeUs / 1000L,
            durationMs = durationMs,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs
        )
    }

    override fun isUnityUntil(presentationTimeUs: Long, sampleRate: Int): Long = Long.MIN_VALUE
}
