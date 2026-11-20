package com.example.data

import com.example.engine.WordTiming
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The offline (Vosk) caption path: recognised words become phrase segments
 * with real start/end times, ready for overlaysFromTranscript's timeline math.
 */
class AutoCaptionSegmentsTest {

    private fun word(text: String, start: Long, end: Long) = WordTiming(text, start, end)

    @Test
    fun `speech pauses split the words into segments`() {
        val segments = AutoCaptionManager.segmentsFromWords(
            listOf(
                word("আমি", 200, 600),
                word("ভালো", 700, 1_000),
                word("আছি", 1_100, 1_400),
                // 2 s pause: a new phrase starts here.
                word("ধন্যবাদ", 3_400, 3_900)
            )
        )
        assertEquals(2, segments.size)
        assertEquals("আমি ভালো আছি", segments[0].text)
        assertEquals(200L, segments[0].startMs)
        assertEquals(1_400L, segments[0].endMs)
        assertEquals("ধন্যবাদ", segments[1].text)
        assertEquals(3_400L, segments[1].startMs)
    }

    @Test
    fun `long phrases break on the word budget`() {
        val words = (0 until 12).map { i ->
            word("w$i", i * 200L, i * 200L + 150L)
        }
        val segments = AutoCaptionManager.segmentsFromWords(words, pauseMs = 650L, maxWords = 5)
        assertEquals(3, segments.size)
        assertTrue(segments.all { it.text.split(" ").size <= 5 })
    }

    @Test
    fun `insane words are dropped and order is preserved`() {
        val segments = AutoCaptionManager.segmentsFromWords(
            listOf(
                word("", 100, 200),
                word("ghost", 900, 500), // end before start
                word("বাংলা", 1_000, 1_200),
                word("ভাষা", 1_300, 1_600)
            )
        )
        assertEquals(1, segments.size)
        assertEquals("বাংলা ভাষা", segments[0].text)
    }

    @Test
    fun `silence produces no segments`() {
        assertTrue(AutoCaptionManager.segmentsFromWords(emptyList()).isEmpty())
    }

    @Test
    fun `segments feed overlaysFromTranscript with trim and speed`() {
        // Words are relative to the decoded span (starting at the clip trim):
        // speech at 1-2 s of a clip trimmed to 500 ms, played at 2x speed.
        val segments = AutoCaptionManager.segmentsFromWords(
            listOf(word("দ্রুত", 1_000, 1_400), word("কথা", 1_500, 1_800))
        )
        val overlays = AutoCaptionManager.overlaysFromTranscript(
            segments = segments,
            sourceOffsetMs = 500L,
            clipStartMs = 2_000L,
            trimStartMs = 500L,
            speed = 2f,
            clipDurationMs = 10_000L
        )
        assertTrue(overlays.isNotEmpty())
        // (500 + 1000 - 500) / 2 = 500 ms local -> 2000 + 500 = 2500 on the timeline.
        assertEquals(2_500L, overlays[0].startTimelineMs)
    }
}
