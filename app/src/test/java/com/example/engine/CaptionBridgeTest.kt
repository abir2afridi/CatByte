package com.example.engine

import com.example.model.TextAnimationType
import com.example.model.TextFontStyle
import com.example.model.TextOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionBridgeTest {

    private var idCounter = 0
    private val idFactory: () -> String = { "test-id-${++idCounter}" }

    @Test
    fun `cues become bottom centred subtitle overlays`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(SubtitleCue(1_000L, 3_000L, "  Hello Android  ")),
            idFactory
        )
        assertEquals(1, overlays.size)
        val overlay = overlays.single()
        assertEquals("Hello Android", overlay.text)
        assertEquals(1_000L, overlay.startTimelineMs)
        assertEquals(2_000L, overlay.durationMs)
        assertEquals(0.5f, overlay.posX)
        assertEquals(CaptionBridge.POS_Y, overlay.posY)
        assertEquals(TextFontStyle.SUBTITLE, overlay.fontStyle)
        assertEquals(TextAnimationType.NONE, overlay.animation)
        assertEquals(CaptionBridge.DEFAULT_BACKGROUND, overlay.backgroundColor)
    }

    @Test
    fun `blank cues are skipped and never become invisible overlays`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(
                SubtitleCue(0L, 1_000L, "   "),
                SubtitleCue(2_000L, 3_000L, "kept")
            ),
            idFactory
        )
        assertEquals(1, overlays.size)
        assertEquals("kept", overlays.single().text)
    }

    @Test
    fun `negative starts are clamped to zero`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(SubtitleCue(-500L, 2_000L, "late")),
            idFactory
        )
        assertEquals(0L, overlays.single().startTimelineMs)
    }

    @Test
    fun `inverted cue is coerced to a positive span`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(SubtitleCue(2_000L, 1_000L, "backwards")),
            idFactory
        )
        val overlay = overlays.single()
        assertEquals(2_000L, overlay.startTimelineMs)
        assertEquals(100L, overlay.durationMs)
    }

    @Test
    fun `absurdly long cues are clamped`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(SubtitleCue(0L, 900_000L, "very long")),
            idFactory
        )
        assertEquals(CaptionBridge.MAX_CAPTION_MS, overlays.single().durationMs)
    }

    @Test
    fun `cues are returned in start order even when the input is shuffled`() {
        val overlays = CaptionBridge.overlaysFromCues(
            listOf(
                SubtitleCue(5_000L, 6_000L, "third"),
                SubtitleCue(1_000L, 2_000L, "first"),
                SubtitleCue(3_000L, 4_000L, "second")
            ),
            idFactory
        )
        assertEquals(listOf("first", "second", "third"), overlays.map { it.text })
    }

    @Test
    fun `overlays become cues and skip blanks`() {
        val overlays = listOf(
            TextOverlay(
                id = "b",
                text = "second",
                startTimelineMs = 4_000L,
                durationMs = 1_500L
            ),
            TextOverlay(id = "blank", text = "  ", startTimelineMs = 0L, durationMs = 500L),
            TextOverlay(
                id = "a",
                text = "first",
                startTimelineMs = 1_000L,
                durationMs = 2_000L
            )
        )
        val cues = CaptionBridge.cuesFromOverlays(overlays)
        assertEquals(2, cues.size)
        assertEquals("first", cues[0].text)
        assertEquals(1_000L, cues[0].startMs)
        assertEquals(3_000L, cues[0].endMs)
        assertEquals("second", cues[1].text)
        assertEquals(4_000L, cues[1].startMs)
        assertEquals(5_500L, cues[1].endMs)
    }

    @Test
    fun `overlay with zero duration exports as a minimal positive cue`() {
        val cues = CaptionBridge.cuesFromOverlays(
            listOf(TextOverlay(id = "x", text = "tick", startTimelineMs = 2_000L, durationMs = 0L))
        )
        assertEquals(100L, cues.single().durationMs)
        assertTrue(cues.single().endMs > cues.single().startMs)
    }

    @Test
    fun `cues round trip through overlays without losing text or order`() {
        val original = listOf(
            SubtitleCue(500L, 1_500L, "alpha"),
            SubtitleCue(2_000L, 3_500L, "beta")
        )
        val overlays = CaptionBridge.overlaysFromCues(original, idFactory)
        // Each import must mint new ids; parity of ids is not expected.
        assertNotEquals(overlays[0].id, overlays[1].id)
        val rebuilt = CaptionBridge.cuesFromOverlays(overlays)
        assertEquals(original, rebuilt)
    }

    @Test
    fun `issues flags overlapping cues and zero length cues`() {
        val overlap = listOf(
            SubtitleCue(0L, 2_000L, "one"),
            SubtitleCue(1_000L, 3_000L, "two")
        )
        assertTrue(CaptionBridge.issues(overlap, projectDurationMs = 10_000L).isNotEmpty())

        val clean = listOf(
            SubtitleCue(0L, 1_000L, "one"),
            SubtitleCue(1_500L, 3_000L, "two")
        )
        assertTrue(CaptionBridge.issues(clean, projectDurationMs = 10_000L).isEmpty())
    }

    @Test
    fun `issues flags cues that start after the project ends`() {
        val cues = listOf(SubtitleCue(20_000L, 21_000L, "too late"))
        assertTrue(CaptionBridge.issues(cues, projectDurationMs = 10_000L).isNotEmpty())
    }

    @Test
    fun `empty project has no issues`() {
        assertTrue(CaptionBridge.issues(emptyList(), projectDurationMs = 0L).isEmpty())
        assertFalse(CaptionBridge.issues(listOf(SubtitleCue(0L, 100L, "t")), 10_000L).isNotEmpty())
    }
}
