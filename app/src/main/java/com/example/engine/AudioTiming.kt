package com.example.engine

import com.example.model.AudioClip
import kotlin.math.roundToLong

/**
 * Canonical timing rules for an independent audio clip.
 *
 * `AudioClip.durationMs` is the duration on the *output* timeline after trim
 * and speed. The Media3 composition, the timeline width and the mixer UI all
 * use this object so a track cannot be 2x in the editor but 1x in the export.
 */
object AudioTiming {

    /** A source window shorter than this is not useful/renderable. */
    const val MIN_AUDIO_DURATION_MS = 100L
    const val MIN_SPEED = 0.1f
    const val MAX_SPEED = 16f

    /** Clamps a requested speed to the range supported by the editor/export. */
    fun normalizedSpeed(speed: Float): Float =
        if (!speed.isFinite()) 1f else speed.coerceIn(MIN_SPEED, MAX_SPEED)

    /**
     * Full, untrimmed source duration.
     *
     * Legacy projects did not persist this value. Their source duration is
     * inferred from the stored output duration, speed and trim points.
     */
    fun sourceDurationMs(audio: AudioClip): Long {
        if (audio.originalDurationMs > 0L) return audio.originalDurationMs
        val trimStart = audio.trimStartMs.coerceAtLeast(0L)
        val speed = normalizedSpeed(audio.speed)
        val inferredFromOutput = trimStart +
            (audio.durationMs.coerceAtLeast(MIN_AUDIO_DURATION_MS) * speed).roundToLong()
        return maxOf(audio.trimEndMs.coerceAtLeast(0L), inferredFromOutput)
            .coerceAtLeast(MIN_AUDIO_DURATION_MS)
    }

    /** Absolute source in-point in milliseconds. */
    fun sourceStartMs(audio: AudioClip): Long = audio.trimStartMs.coerceAtLeast(0L)

    /** Absolute source out-point; the source end when the trim is open. */
    fun sourceEndMs(audio: AudioClip): Long {
        val sourceEnd = sourceDurationMs(audio)
        val explicitEnd = audio.trimEndMs
        return if (explicitEnd > sourceStartMs(audio)) {
            explicitEnd.coerceIn(sourceStartMs(audio), sourceEnd)
        } else {
            sourceEnd
        }
    }

    /** Duration of the selected source window before speed is applied. */
    fun sourceWindowMs(audio: AudioClip): Long =
        (sourceEndMs(audio) - sourceStartMs(audio)).coerceAtLeast(MIN_AUDIO_DURATION_MS)

    /** Duration on the output timeline after trim and speed. */
    fun presentationDurationMs(audio: AudioClip): Long {
        val speed = normalizedSpeed(audio.speed)
        return (sourceWindowMs(audio) / speed).roundToLong()
            .coerceAtLeast(MIN_AUDIO_DURATION_MS)
    }

    /**
     * Clamps a requested source trim to the available source and keeps at least
     * [MIN_AUDIO_DURATION_MS] of source material.
     */
    fun clampTrim(
        audio: AudioClip,
        requestedStartMs: Long,
        requestedEndMs: Long
    ): Pair<Long, Long> {
        val sourceDuration = sourceDurationMs(audio)
        val latestStart = (sourceDuration - MIN_AUDIO_DURATION_MS).coerceAtLeast(0L)
        val start = requestedStartMs.coerceIn(0L, latestStart)
        val requestedEnd = if (requestedEndMs <= 0L) {
            sourceDuration
        } else {
            requestedEndMs
        }
        val earliestEnd = (start + MIN_AUDIO_DURATION_MS).coerceAtMost(sourceDuration)
        val end = requestedEnd.coerceIn(earliestEnd, sourceDuration)
        return start to end
    }

    /**
     * Clamps fade durations to the output clip. If the two fades would overlap,
     * both are scaled proportionally so the envelope never multiplies twice.
     */
    fun clampFades(
        durationMs: Long,
        requestedFadeInMs: Long,
        requestedFadeOutMs: Long
    ): Pair<Long, Long> {
        val duration = durationMs.coerceAtLeast(MIN_AUDIO_DURATION_MS)
        val fadeIn = requestedFadeInMs.coerceAtLeast(0L)
        val fadeOut = requestedFadeOutMs.coerceAtLeast(0L)
        val total = fadeIn + fadeOut
        if (total <= duration) return fadeIn to fadeOut

        val scale = duration.toDouble() / total.toDouble()
        return (fadeIn * scale).roundToLong() to (fadeOut * scale).roundToLong()
    }
}
