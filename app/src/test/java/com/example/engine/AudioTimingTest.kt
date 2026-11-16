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
        fadeOutMs: Long = 0L
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
        fadeOutMs = fadeOutMs
    )

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
        assertEquals(16f, AudioTiming.normalizedSpeed(99f), 0.0001f)
        assertEquals(1f, AudioTiming.normalizedSpeed(Float.NaN), 0.0001f)
    }

    @Test
    fun `overlapping fades are scaled instead of multiplied`() {
        val (fadeIn, fadeOut) = AudioTiming.clampFades(1_000L, 800L, 600L)
        assertEquals(1_000L, fadeIn + fadeOut)
        assertEquals(571L, fadeIn)
        assertEquals(429L, fadeOut)
    }
}
