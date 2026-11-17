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

/**
 * Constant speed normalised to an exact output duration.
 *
 * Media3 derives the presentation duration from the clipped source duration
 * divided by this provider's speed. Normalising here removes the sub-ms
 * rounding drift between the mixer timeline and the exported file.
 */
@UnstableApi
class ConstantAudioSpeedProvider(
    requestedSpeed: Float,
    clippedSourceDurationUs: Long,
    targetOutputDurationUs: Long
) : SpeedProvider {
    private val effectiveSpeed = if (targetOutputDurationUs > 0L) {
        (clippedSourceDurationUs.toDouble() / targetOutputDurationUs.toDouble())
            .toFloat()
            .coerceIn(0.01f, 64f)
    } else {
        requestedSpeed.coerceIn(0.01f, 64f)
    }

    override fun getSpeed(presentationTimeUs: Long): Float = effectiveSpeed
    override fun getNextSpeedChangeTimeUs(presentationTimeUs: Long): Long = C.TIME_UNSET
}

/**
 * Applies a constant volume with optional fade in / fade out.
 *
 * Media3 addresses the provider in SAMPLE FRAMES, so the millisecond maths
 * runs through [AudioMath.samplePositionToMs]. Treating the frame counter as
 * microseconds would stretch every fade by the sample rate (48x at 48 kHz),
 * which is why the position is converted explicitly.
 */
@UnstableApi
class ConstantGainProvider(
    private val gain: Float,
    private val fadeInMs: Long,
    private val fadeOutMs: Long,
    private val durationMs: Long
) : GainProcessor.GainProvider {

    override fun getGainFactorAtSamplePosition(samplePosition: Long, sampleRate: Int): Float {
        val base = gain.coerceIn(0f, 2f)
        if (durationMs <= 0L) return base
        // Shared with the preview mixer (AudioMath) so fades match exactly.
        return base * AudioMath.fadeGain(
            positionMs = AudioMath.samplePositionToMs(samplePosition, sampleRate),
            durationMs = durationMs,
            fadeInMs = fadeInMs,
            fadeOutMs = fadeOutMs
        )
    }

    /**
     * The gain is unity from the end of the fade-in to the start of the
     * fade-out. Media3 uses this to copy unity samples in bulk, so the answer
     * has to be the exact frame where the fade-out begins.
     */
    override fun isUnityUntil(samplePosition: Long, sampleRate: Int): Long {
        if (fadeOutMs <= 0L) return C.TIME_END_OF_SOURCE
        val unityEndMs = (durationMs - fadeOutMs).coerceAtLeast(0L)
        val unityEnd = AudioMath.msToSamplePosition(unityEndMs, sampleRate)
        // Never report an end before the current position: Media3 would try to
        // shrink the input buffer below its position.
        return maxOf(unityEnd, samplePosition)
    }
}

/**
 * Music gain that automatically dips while a voice-over is audible.
 *
 * [startTimelineMs] lifts the item-relative sample position onto the project
 * timeline, and [voiceRanges] are the timeline spans of audible voice-over
 * clips. The attack/release curve lives in [AudioMath.duckEnvelope], so the
 * preview mixer and the export muxer dip by exactly the same amount.
 */
@UnstableApi
class DuckGainProvider(
    private val base: ConstantGainProvider,
    private val startTimelineMs: Long,
    /** Timeline spans where the voice-over is audible. */
    val voiceRanges: List<LongRange>,
    /** How deep the music dips (0 = full level, 1 = silent). */
    val duckAmount: Float = 0.35f
) : GainProcessor.GainProvider {

    override fun getGainFactorAtSamplePosition(samplePosition: Long, sampleRate: Int): Float {
        val baseGain = base.getGainFactorAtSamplePosition(samplePosition, sampleRate)
        if (voiceRanges.isEmpty()) return baseGain
        val timelineMs = startTimelineMs + AudioMath.samplePositionToMs(samplePosition, sampleRate)
        return baseGain * AudioMath.duckEnvelope(
            positionMs = timelineMs,
            voiceRanges = voiceRanges,
            duckAmount = duckAmount
        )
    }

    /**
     * Unity lasts until the duck attack starts or the base fade-out begins.
     * Both bounds are reported in sample frames, and an unbounded end is
     * Media3's TIME_END_OF_SOURCE.
     */
    override fun isUnityUntil(samplePosition: Long, sampleRate: Int): Long {
        val timelineMs = startTimelineMs + AudioMath.samplePositionToMs(samplePosition, sampleRate)
        // The duck starts ramping up ATTACK_MS before the voice-over begins.
        val nextAttackMs = voiceRanges
            .map { it.first - AudioMath.DUCK_ATTACK_MS }
            .firstOrNull { it > timelineMs }
        val duckEnd = if (nextAttackMs == null) {
            C.TIME_END_OF_SOURCE
        } else {
            AudioMath.msToSamplePosition(nextAttackMs - startTimelineMs, sampleRate)
        }
        val baseEnd = base.isUnityUntil(samplePosition, sampleRate)
        return earlierUnityEnd(duckEnd, baseEnd)
    }
}

/**
 * Picks the earlier of two unity-region ends. Media3 marks "unity until the
 * end of the stream" with TIME_END_OF_SOURCE, which is numerically smaller
 * than every real position, so it has to be handled separately.
 */
internal fun earlierUnityEnd(first: Long, second: Long): Long {
    val firstUnbounded = first == C.TIME_END_OF_SOURCE
    val secondUnbounded = second == C.TIME_END_OF_SOURCE
    return when {
        firstUnbounded && secondUnbounded -> C.TIME_END_OF_SOURCE
        firstUnbounded -> second
        secondUnbounded -> first
        else -> minOf(first, second)
    }
}
