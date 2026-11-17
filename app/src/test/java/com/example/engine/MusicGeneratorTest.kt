package com.example.engine

import com.example.data.SampleAudioGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

/**
 * "Audio → AI music": the procedural composer must render real, normalised,
 * deterministic audio for every mood, and the WAV writer must accept it.
 */
class MusicGeneratorTest {

    @Test
    fun `every mood renders the requested duration`() {
        MusicGenerator.Mood.entries.forEach { mood ->
            val audio = MusicGenerator.render(mood, durationMs = 4_000L, seed = 42L)
            assertEquals(4_000L, audio.durationMs)
            assertEquals((4_000L * SampleAudioGenerator.SAMPLE_RATE / 1_000L).toInt(), audio.samples.size)
        }
    }

    @Test
    fun `the render is not silent and not clipping`() {
        val audio = MusicGenerator.render(MusicGenerator.Mood.UPBEAT, 6_000L, seed = 7L)
        val peak = audio.samples.maxOf { abs(it.toInt()) }
        assertTrue("peak $peak must be audible", peak > 2_000)
        assertTrue("peak $peak must stay in range", peak <= Short.MAX_VALUE.toInt())
    }

    @Test
    fun `moods sound different from each other`() {
        val renderLength = 8_000
        val renders = MusicGenerator.Mood.entries.map { mood ->
            MusicGenerator.render(mood, 5_000L, seed = 1L).samples.copyOf(renderLength)
        }
        val reference = renders[0]
        MusicGenerator.Mood.entries.drop(1).forEachIndexed { index, mood ->
            val other = renders[index + 1]
            val differing = reference.indices.count { reference[it] != other[it] }
            assertTrue("${mood.name} must differ from CHILL", differing > renderLength / 2)
        }
    }

    @Test
    fun `the same seed renders the same music`() {
        val a = MusicGenerator.render(MusicGenerator.Mood.CINEMATIC, 3_000L, seed = 99L)
        val b = MusicGenerator.render(MusicGenerator.Mood.CINEMATIC, 3_000L, seed = 99L)
        assertTrue(a.samples.contentEquals(b.samples))
    }

    @Test
    fun `a different seed changes the groove`() {
        val a = MusicGenerator.render(MusicGenerator.Mood.UPBEAT, 3_000L, seed = 1L)
        val b = MusicGenerator.render(MusicGenerator.Mood.UPBEAT, 3_000L, seed = 2L)
        assertTrue(!a.samples.contentEquals(b.samples))
    }

    @Test
    fun `the render is a valid wav payload`() {
        val audio = MusicGenerator.render(MusicGenerator.Mood.CHILL, 2_000L, seed = 3L)
        val file = File.createTempFile("ai_music", ".wav")
        try {
            SampleAudioGenerator.writeWav(file, audio.samples)
            // 44-byte header + 16-bit mono samples.
            assertEquals(44L + audio.samples.size * 2L, file.length())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `extreme durations are clamped`() {
        val tiny = MusicGenerator.render(MusicGenerator.Mood.CHILL, durationMs = 1L)
        assertTrue(tiny.durationMs >= SampleAudioGenerator.SAMPLE_RATE / 1000L)
        val huge = MusicGenerator.render(MusicGenerator.Mood.UPBEAT, durationMs = 5 * 60_000L)
        assertEquals(60_000L, huge.durationMs)
    }
}
