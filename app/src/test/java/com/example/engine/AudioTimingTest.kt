package com.example.engine

import com.example.model.AudioClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Canonical trim/speed/fade maths shared by the mixer, preview and export. */
class AudioTimingTest {

    private fun audio(
        durationMs: Long = 10_000L,
        originalDurationMs: Long = 10_000L,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L,
        speed: Float = 1f,
        fadeInMs: Long = 0L,
        fadeOutMs: Long = 0L,
        loopCount: Int = 1
    ) = AudioClip(
        id = "audio",
        title = "Audio",
        artist = "Test",
        uriString = "content://audio/1",
        durationMs = durationMs,
        originalDurationMs = originalDurationMs,
        trimStartMs = trimStartMs,
        trimEndMs = trimEndMs,
        speed = speed,
        fadeInMs = fadeInMs,
        fadeOutMs = fadeOutMs,
        loopCount = loopCount
    )

    @Test
    fun `loop repeats multiply the timeline duration`() {
        val clip = audio(
            durationMs = 2_000L,
            trimStartMs = 2_000L,
            trimEndMs = 6_000L,
            speed = 2f,
            loopCount = 4
        )
        // One pass is 2 s (4 s window at 2x); four passes cover 8 s.
        assertEquals(2_000L, AudioTiming.unitDurationMs(clip))
        assertEquals(8_000L, AudioTiming.presentationDurationMs(clip))
        assertEquals(4, AudioTiming.loopCount(clip))
    }

    @Test
    fun `loop count is clamped to the supported range`() {
        assertEquals(1, AudioTiming.loopCount(audio(loopCount = 0)))
        assertEquals(AudioTiming.MAX_LOOP_COUNT, AudioTiming.loopCount(audio(loopCount = 99)))
    }

    @Test
    fun `legacy source inference uses one pass of a looping track`() {
        val legacy = AudioClip(
            id = "legacy",
            title = "Legacy",
            artist = "Test",
            uriString = "content://audio/legacy",
            durationMs = 8_000L,
            originalDurationMs = 0L,
            loopCount = 4
        )
        // 8 s of output over four repeats means a 2 s source window.
        assertEquals(2_000L, AudioTiming.sourceDurationMs(legacy))
    }

    @Test
    fun `whole source keeps its duration at normal speed`() {
        assertEquals(10_000L, AudioTiming.presentationDurationMs(audio()))
    }

    @Test
    fun `trim window is divided by speed`() {
        val clip = audio(
            durationMs = 2_000L,
            trimStartMs = 2_000L,
            trimEndMs = 6_000L,
            speed = 2f
        )
        assertEquals(4_000L, AudioTiming.sourceWindowMs(clip))
        assertEquals(2_000L, AudioTiming.presentationDurationMs(clip))
    }

    @Test
    fun `legacy clip infers source duration from output duration and speed`() {
        val clip = audio(
            durationMs = 4_000L,
            originalDurationMs = 0L,
            speed = 2f
        )
        assertEquals(8_000L, AudioTiming.sourceDurationMs(clip))
        assertEquals(4_000L, AudioTiming.presentationDurationMs(clip))
    }

    @Test
    fun `invalid trim is clamped to the available source`() {
        val clip = audio(originalDurationMs = 5_000L)
        val (start, end) = AudioTiming.clampTrim(clip, -500L, 9_000L)
        assertEquals(0L, start)
        assertEquals(5_000L, end)
    }

    @Test
    fun `trim always leaves a usable source window`() {
        val clip = audio(originalDurationMs = 5_000L)
        val (start, end) = AudioTiming.clampTrim(clip, 4_999L, 4_999L)
        assertTrue(end > start)
        assertTrue(end <= 5_000L)
    }

    @Test
    fun `speed is clamped to the supported range`() {
        assertEquals(0.1f, AudioTiming.normalizedSpeed(0f), 0.0001f)
        assertEquals(32f, AudioTiming.normalizedSpeed(99f), 0.0001f)
        assertEquals(1f, AudioTiming.normalizedSpeed(Float.NaN), 0.0001f)
    }

    @Test
    fun `overlapping fades are scaled instead of multiplied`() {
        val (fadeIn, fadeOut) = AudioTiming.clampFades(1_000L, 800L, 600L)
        assertEquals(1_000L, fadeIn + fadeOut)
        assertEquals(571L, fadeIn)
        assertEquals(429L, fadeOut)
    }

    @Test
    fun `malformed trim outside the source is normalized without crashing`() {
        val clip = audio(
            durationMs = 1_000L,
            originalDurationMs = 1_000L,
            trimStartMs = 5_000L,
            trimEndMs = 6_000L
        )

        val normalized = AudioTiming.normalize(clip)

        assertEquals(900L, normalized.trimStartMs)
        assertEquals(1_000L, normalized.trimEndMs)
        assertEquals(100L, normalized.durationMs)
    }

    @Test
    fun `legacy normalization materialises source duration and canonical output duration`() {
        val clip = audio(
            durationMs = 4_000L,
            originalDurationMs = 0L,
            speed = 2f
        )

        val normalized = AudioTiming.normalize(clip)

        assertEquals(8_000L, normalized.originalDurationMs)
        assertEquals(4_000L, normalized.durationMs)
        assertEquals(0L, normalized.trimEndMs)
    }

    @Test
    fun `normalization replaces non finite volume and clamps fades`() {
        val clip = audio(
            durationMs = 1_000L,
            originalDurationMs = 1_000L,
            fadeInMs = Long.MAX_VALUE,
            fadeOutMs = Long.MAX_VALUE
        ).copy(volume = Float.NaN)

        val normalized = AudioTiming.normalize(clip)

        assertEquals(1f, normalized.volume, 0.0001f)
        assertEquals(1_000L, normalized.fadeInMs + normalized.fadeOutMs)
    }

    @Test
    fun `legacy placeholder waveform is dropped on normalization`() {
        val clip = audio(durationMs = 4_000L, originalDurationMs = 4_000L).copy(
            waveform = listOf(0.2f, 0.5f, 0.8f, 0.9f, 0.6f, 0.4f, 0.7f, 0.9f, 0.8f, 0.5f, 0.3f, 0.6f)
        )

        val normalized = AudioTiming.normalize(clip)

        assertTrue(normalized.waveform.isEmpty())
    }

    @Test
    fun `real waveforms survive normalization with malformed values cleaned`() {
        val clip = audio(durationMs = 4_000L, originalDurationMs = 4_000L).copy(
            waveform = listOf(0.25f, 0.75f, 1.4f, Float.NaN, -0.2f)
        )

        val normalized = AudioTiming.normalize(clip)

        assertEquals(listOf(0.25f, 0.75f, 1f, 0f), normalized.waveform)
    }
}
