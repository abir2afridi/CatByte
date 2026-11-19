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
    const val MAX_SPEED = 32f

    /** The loudest repeat count the editor renders (keeps the graph sane). */
    const val MAX_LOOP_COUNT = 16

    /** Clamps a requested speed to the range supported by the editor/export. */
    fun normalizedSpeed(speed: Float): Float =
        if (!speed.isFinite()) 1f else speed.coerceIn(MIN_SPEED, MAX_SPEED)

    /** How often the trimmed source window repeats (1 = play it once). */
    fun loopCount(audio: AudioClip): Int = audio.loopCount.coerceIn(1, MAX_LOOP_COUNT)

    /**
     * Full, untrimmed source duration.
     *
     * Legacy projects did not persist this value. Their source duration is
     * inferred from the stored output duration, speed and trim points. The
     * result is always materialised before a speed/trim mutation, so changing
     * the speed can never reinterpret the same legacy source as a shorter one.
     */
    fun sourceDurationMs(audio: AudioClip): Long {
        if (audio.originalDurationMs > 0L) {
            return audio.originalDurationMs.coerceAtLeast(MIN_AUDIO_DURATION_MS)
        }
        val trimStart = audio.trimStartMs.coerceAtLeast(0L)
        val speed = normalizedSpeed(audio.speed)
        // The stored duration covers every repeat, so the inference has to
        // look at ONE pass of the window.
        val onePassMs = (audio.durationMs / loopCount(audio)).coerceAtLeast(MIN_AUDIO_DURATION_MS)
        val scaledOutput = scaleDurationMs(
            durationMs = onePassMs,
            speed = speed
        )
        val inferredFromOutput = saturatingAdd(trimStart, scaledOutput)
        return maxOf(audio.trimEndMs.coerceAtLeast(0L), inferredFromOutput)
            .coerceAtLeast(MIN_AUDIO_DURATION_MS)
    }

    /** Absolute source in-point in milliseconds. */
    fun sourceStartMs(audio: AudioClip): Long = audio.trimStartMs.coerceAtLeast(0L)

    /** Absolute source out-point; the source end when the trim is open. */
    fun sourceEndMs(audio: AudioClip): Long {
        val sourceEnd = sourceDurationMs(audio)
        val start = sourceStartMs(audio)
        val explicitEnd = audio.trimEndMs
        // Never call coerceIn with reversed bounds: malformed persisted JSON
        // used to crash preview/export with IllegalArgumentException.
        return when {
            explicitEnd <= start -> sourceEnd
            explicitEnd < sourceEnd -> explicitEnd
            else -> sourceEnd
        }
    }

    /** Duration of the selected source window before speed is applied. */
    fun sourceWindowMs(audio: AudioClip): Long =
        (sourceEndMs(audio) - sourceStartMs(audio)).coerceAtLeast(MIN_AUDIO_DURATION_MS)

    /** Duration of ONE pass of the selected source window before looping. */
    fun unitDurationMs(audio: AudioClip): Long {
        val speed = normalizedSpeed(audio.speed)
        return (sourceWindowMs(audio).toDouble() / speed.toDouble())
            .roundToLong()
            .coerceAtLeast(MIN_AUDIO_DURATION_MS)
    }

    /**
     * Duration on the output timeline after trim, speed and loop repeats.
     *
     * A looping track covers `loopCount` times the source window, so the
     * timeline, the mixer and the export agree on its length.
     */
    fun presentationDurationMs(audio: AudioClip): Long {
        val unit = unitDurationMs(audio).toDouble()
        return (unit * loopCount(audio).toDouble())
            .roundToLong()
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
        val fadeIn = requestedFadeInMs.coerceIn(0L, duration)
        val fadeOut = requestedFadeOutMs.coerceIn(0L, duration)
        // Subtraction avoids overflowing when a malformed JSON value is
        // Long.MAX_VALUE.
        if (fadeIn <= duration - fadeOut) return fadeIn to fadeOut

        val scale = duration.toDouble() / (fadeIn.toDouble() + fadeOut.toDouble())
        return (fadeIn * scale).roundToLong() to (fadeOut * scale).roundToLong()
    }

    /**
     * Sanitises an audio clip into the canonical persisted/rendered shape.
     * This is the single migration path for legacy and malformed projects:
     * source duration is explicit, trim points are real source coordinates,
     * speed/volume are finite and bounded, fades fit, and duration matches
     * the selected source window after speed.
     */
    fun normalize(audio: AudioClip): AudioClip {
        val speed = normalizedSpeed(audio.speed)
        val sourceDuration = sourceDurationMs(audio)
        val (trimStart, clampedTrimEnd) = clampTrim(
            audio = audio.copy(originalDurationMs = sourceDuration, speed = speed),
            requestedStartMs = audio.trimStartMs,
            requestedEndMs = audio.trimEndMs
        )
        // Zero keeps the model's "until the source ends" meaning. Explicit
        // in/out points are persisted as real source coordinates.
        val trimEnd = if (audio.trimEndMs <= 0L) 0L else clampedTrimEnd
        val canonical = audio.copy(
            startTimelineMs = audio.startTimelineMs.coerceAtLeast(0L),
            originalDurationMs = sourceDuration,
            trimStartMs = trimStart,
            trimEndMs = trimEnd,
            speed = speed,
            volume = if (audio.volume.isFinite()) audio.volume.coerceIn(0f, 2f) else 1f,
            pan = StereoPan.clamp(audio.pan),
            equalizer = audio.equalizer.sanitized(),
            loopCount = loopCount(audio),
            waveform = sanitizedWaveform(audio.waveform)
        )
        val duration = presentationDurationMs(canonical)
        val fades = clampFades(duration, canonical.fadeInMs, canonical.fadeOutMs)
        return canonical.copy(
            durationMs = duration,
            fadeInMs = fades.first,
            fadeOutMs = fades.second
        )
    }

    private fun scaleDurationMs(durationMs: Long, speed: Float): Long {
        val scaled = durationMs.toDouble() * speed.toDouble()
        return if (!scaled.isFinite() || scaled >= Long.MAX_VALUE.toDouble()) {
            Long.MAX_VALUE
        } else {
            scaled.roundToLong().coerceAtLeast(0L)
        }
    }

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > Long.MAX_VALUE - left) Long.MAX_VALUE else left + right

    /**
     * Waveform migration. Legacy builds shipped a decorative placeholder
     * envelope as the [AudioClip.waveform] default; it does not describe the
     * real audio, so it is dropped here like the other legacy fields. Real
     * envelopes are kept and clamped to the 0..1 bar range.
     */
    private val LEGACY_PLACEHOLDER_WAVEFORM = listOf(
        0.2f, 0.5f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.8f, 0.5f, 0.3f, 0.6f
    )

    private fun sanitizedWaveform(waveform: List<Float>): List<Float> {
        if (waveform.isEmpty() || waveform == LEGACY_PLACEHOLDER_WAVEFORM) {
            return emptyList()
        }
        var clean = true
        for (value in waveform) {
            if (!value.isFinite() || value < 0f || value > 1f) {
                clean = false
                break
            }
        }
        if (clean) return waveform
        return waveform
            .filter { it.isFinite() }
            .map { it.coerceIn(0f, 1f) }
    }
}
