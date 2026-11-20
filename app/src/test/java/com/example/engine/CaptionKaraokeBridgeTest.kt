package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transcription → karaoke captions path: word timings must survive into
 * the overlays as cue-relative karaoke words.
 */
class CaptionKaraokeBridgeTest {

    @Test
    fun `timeline cues become karaoke overlays with relative word times`() {
        val cue = CaptionTimeline.Cue(
            startMs = 2_000,
            endMs = 4_000,
            words = listOf(
                CaptionTimeline.Word("hello", 2_000, 2_500),
                CaptionTimeline.Word("world", 2_600, 3_400)
            )
        )
        val overlays = CaptionBridge.overlaysFromTimelineCues(listOf(cue))
        assertEquals(1, overlays.size)
        val overlay = overlays[0]
        assertEquals("hello world", overlay.text)
        assertTrue(overlay.karaokeEnabled)
        assertEquals(2, overlay.karaokeWords.size)
        // Times are cue-relative, not absolute.
        assertEquals(0L, overlay.karaokeWords[0].startMs)
        assertEquals(600L, overlay.karaokeWords[1].startMs)
        // The active word lookup works against the overlay's own clock.
        assertEquals(0, overlay.activeKaraokeIndex(200))
        assertEquals(1, overlay.activeKaraokeIndex(700))
    }

    @Test
    fun `extendForReadability keeps karaoke words aligned`() {
        val words = listOf(
            CaptionTimeline.Word("one", 0, 300),
            CaptionTimeline.Word("two", 400, 700)
        )
        val cues = CaptionTimeline.cues(words, maxChars = 20, maxWords = 5)
        val extended = CaptionTimeline.extendForReadability(cues, targetCps = 5f, gapMs = 50L)
        val overlays = CaptionBridge.overlaysFromTimelineCues(extended)
        // Extended end must not shift the words, only the cue tail.
        assertTrue(overlays[0].durationMs >= cues[0].durationMs)
        assertEquals(0L, overlays[0].karaokeWords[0].startMs)
        assertEquals(400L, overlays[0].karaokeWords[1].startMs)
    }

    @Test
    fun `srt import stays plain (no karaoke words)`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(SubtitleCue(0, 2_000, "just a line"))
        )
        assertTrue(overlays[0].karaokeWords.isEmpty())
        assertTrue(!overlays[0].karaokeEnabled)
    }
}
