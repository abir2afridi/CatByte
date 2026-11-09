package com.example.data

import com.example.engine.CaptionBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCaptionManagerTest {

    @Test
    fun `long phrases become several readable cues`() = runBlocking {
        val overlays = AutoCaptionManager.generateCaptions(15_000L, "English")
        assertTrue("expected multiple cues", overlays.size >= 3)
        assertTrue(overlays.all { it.text.length <= AutoCaptionManager.MAX_CHARS_PER_CUE })
        assertTrue(overlays.all { it.text.isNotBlank() })
        assertTrue(overlays.all { it.durationMs >= 100L })
    }

    @Test
    fun `cues are ordered and never overlap`() = runBlocking {
        val overlays = AutoCaptionManager.generateCaptions(15_000L, "Bengali")
        overlays.zipWithNext().forEach { (first, second) ->
            assertTrue(
                "cue at ${second.startTimelineMs} overlaps the one before it",
                second.startTimelineMs >= first.startTimelineMs + first.durationMs
            )
        }
    }

    @Test
    fun `captions stay inside the project duration`() = runBlocking {
        val overlays = AutoCaptionManager.generateCaptions(4_000L, "Bengali")
        assertTrue(overlays.isNotEmpty())
        overlays.forEach { overlay ->
            assertTrue(
                "caption ends at ${overlay.startTimelineMs + overlay.durationMs}",
                overlay.startTimelineMs + overlay.durationMs <= 4_000L
            )
        }
    }

    @Test
    fun `captions are bottom centred subtitles`() = runBlocking {
        val overlays = AutoCaptionManager.generateCaptions(8_000L)
        assertEquals(CaptionBridge.POS_Y, overlays.first().posY)
        assertEquals(CaptionBridge.DEFAULT_BACKGROUND, overlays.first().backgroundColor)
    }

    @Test
    fun `empty and negative durations produce nothing`() = runBlocking {
        assertTrue(AutoCaptionManager.generateCaptions(0L).isEmpty())
        assertTrue(AutoCaptionManager.generateCaptions(-1_000L).isEmpty())
    }

    @Test
    fun `a project shorter than one phrase still gets a caption`() = runBlocking {
        val overlays = AutoCaptionManager.generateCaptions(1_500L)
        assertTrue(overlays.isNotEmpty())
        assertTrue(overlays.all { it.startTimelineMs + it.durationMs <= 1_500L })
    }
}
