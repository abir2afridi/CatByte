package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionFilterTest {

    private fun words(vararg texts: String, stepMs: Long = 300, durMs: Long = 250): List<CaptionTimeline.Word> =
        texts.mapIndexed { index, text ->
            CaptionTimeline.Word(text, index * stepMs, index * stepMs + durMs)
        }

    @Test
    fun `cues break on the word budget`() {
        val input = words("w0", "w1", "w2", "w3", "w4", "w5", "w6", "w7", "w8", "w9")
        val cues = CaptionTimeline.cues(input, maxChars = 1_000, maxWords = 4)
        assertEquals(3, cues.size)
        assertEquals(4, cues[0].words.size)
        assertEquals(4, cues[1].words.size)
        assertEquals(2, cues[2].words.size)
    }

    @Test
    fun `cues break on the character budget`() {
        val input = words("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff")
        val cues = CaptionTimeline.cues(input, maxChars = 32, maxWords = 10)
        assertEquals(2, cues.size)
        assertEquals(5, cues[0].words.size)
        assertEquals(1, cues[1].words.size)
    }

    @Test
    fun `cues break on a long pause`() {
        val input = words("one", "two", "three", stepMs = 1_500, durMs = 300)
        val cues = CaptionTimeline.cues(input, maxChars = 1_000, maxWords = 10, pauseMs = 700)
        assertEquals(3, cues.size)
    }

    @Test
    fun `empty word list produces no cues`() {
        assertTrue(CaptionTimeline.cues(emptyList()).isEmpty())
    }

    @Test
    fun `karaoke frames are contiguous and cover every word`() {
        val cue = CaptionTimeline.Cue(0, 900, words("a", "b", "c"))
        val frames = CaptionTimeline.karaoke(cue)
        assertEquals(3, frames.size)
        assertEquals(0, frames[0].startMs)
        assertEquals(300, frames[0].endMs)
        assertEquals(frames[0].endMs, frames[1].startMs)
        assertEquals(2, frames[2].activeIndex)
        assertEquals(900, frames[2].endMs)
    }

    @Test
    fun `active index follows the playhead`() {
        val cue = CaptionTimeline.Cue(0, 900, words("a", "b", "c"))
        assertEquals(0, CaptionTimeline.activeIndex(cue, 10))
        assertEquals(2, CaptionTimeline.activeIndex(cue, 650))
        assertNull(CaptionTimeline.activeIndex(cue, 5_000))
    }

    @Test
    fun `reading speed is characters per second`() {
        val cue = CaptionTimeline.Cue(0, 550, words("ab", "cd"))
        assertEquals(9.09f, CaptionTimeline.readingSpeed(cue), 0.05f)
    }

    @Test
    fun `slow cues are extended for readability`() {
        val cue = CaptionTimeline.Cue(0, 500, listOf(CaptionTimeline.Word("a".repeat(34), 0, 500)))
        val next = CaptionTimeline.Cue(5_000, 5_500, listOf(CaptionTimeline.Word("next", 5_000, 5_500)))
        val extended = CaptionTimeline.extendForReadability(listOf(cue, next), targetCps = 17f)
        assertEquals(2_000, extended[0].endMs)
        assertEquals(5_500, extended[1].endMs)
    }

    @Test
    fun `extension never overlaps the next cue`() {
        val first = CaptionTimeline.Cue(0, 500, listOf(CaptionTimeline.Word("a".repeat(34), 0, 500)))
        val second = CaptionTimeline.Cue(1_200, 1_500, listOf(CaptionTimeline.Word("b", 1_200, 1_500)))
        val extended = CaptionTimeline.extendForReadability(listOf(first, second), targetCps = 17f, gapMs = 80)
        assertEquals(1_120, extended[0].endMs)
    }

    @Test
    fun `preset library has twenty unique bilingual looks`() {
        assertEquals(20, FilterPresets.presets.size)
        assertEquals(20, FilterPresets.presets.map { it.id }.toSet().size)
        assertTrue(FilterPresets.presets.all { it.nameEn.isNotBlank() && it.nameBn.isNotBlank() })
        assertNotNull(FilterPresets.byId("bangla_gold"))
    }

    @Test
    fun `intensity blends from neutral to the full look`() {
        val preset = FilterPresets.presets.first()
        assertEquals(FilterPresets.neutral, FilterPresets.atIntensity(preset, 0f))
        assertEquals(preset.grade, FilterPresets.blend(FilterPresets.neutral, preset.grade, 1f))
        val half = FilterPresets.blend(FilterPresets.neutral, preset.grade, 0.5f)
        assertEquals(preset.grade.contrast / 2f, half.contrast, 0.0001f)
    }
}
