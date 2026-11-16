package com.example.data

import com.example.data.ai.TranscriptSegment
import com.example.engine.CaptionBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI captions arrive in *source* time; these tests pin how they are shifted and
 * clamped onto the timeline (clip start, trim, speed).
 */
class AutoCaptionManagerTest {

    private fun overlays(
        segments: List<TranscriptSegment>,
        sourceOffsetMs: Long = 0L,
        clipStartMs: Long = 0L,
        trimStartMs: Long = 0L,
        speed: Float = 1f,
        clipDurationMs: Long = 10_000L
    ) = AutoCaptionManager.overlaysFromTranscript(
        segments = segments,
        sourceOffsetMs = sourceOffsetMs,
        clipStartMs = clipStartMs,
        trimStartMs = trimStartMs,
        speed = speed,
        clipDurationMs = clipDurationMs
    )

    @Test
    fun `a segment becomes a bottom centred subtitle`() {
        val result = overlays(listOf(TranscriptSegment(1_000L, 3_000L, "hello world")))
        assertEquals(1, result.size)
        assertEquals(1_000L, result.first().startTimelineMs)
        assertEquals(CaptionBridge.POS_Y, result.first().posY)
        assertEquals(CaptionBridge.DEFAULT_BACKGROUND, result.first().backgroundColor)
    }

    @Test
    fun `clip start shifts the caption onto the timeline`() {
        val result = overlays(
            segments = listOf(TranscriptSegment(0L, 2_000L, "second clip")),
            clipStartMs = 7_000L
        )
        assertEquals(7_000L, result.first().startTimelineMs)
    }

    @Test
    fun `trim start is removed from transcript time`() {
        val result = overlays(
            segments = listOf(TranscriptSegment(500L, 1_500L, "after trim")),
            sourceOffsetMs = 4_000L,
            trimStartMs = 4_000L
        )
        assertEquals(500L, result.first().startTimelineMs)
    }

    @Test
    fun `speed compresses source time`() {
        val result = overlays(
            segments = listOf(TranscriptSegment(2_000L, 4_000L, "fast")),
            speed = 2f
        )
        assertEquals(1_000L, result.first().startTimelineMs)
    }

    @Test
    fun `captions never run past the clip`() {
        val result = overlays(
            segments = listOf(TranscriptSegment(8_000L, 20_000L, "long ending")),
            clipDurationMs = 9_000L
        )
        assertTrue(result.isNotEmpty())
        result.forEach { caption ->
            assertTrue(
                "caption ends at ${caption.startTimelineMs + caption.durationMs}",
                caption.startTimelineMs + caption.durationMs <= 9_000L
            )
        }
    }

    @Test
    fun `long phrases split into readable cards`() {
        val result = overlays(
            listOf(
                TranscriptSegment(
                    0L,
                    8_000L,
                    "this is a fairly long sentence that should not be a single wall of text on screen"
                )
            )
        )
        assertTrue("expected several cues", result.size >= 3)
        assertTrue(result.all { it.text.length <= AutoCaptionManager.MAX_CHARS_PER_CUE })
        assertTrue(result.all { it.text.isNotBlank() })
        result.zipWithNext().forEach { (first, second) ->
            assertTrue(second.startTimelineMs >= first.startTimelineMs + first.durationMs)
        }
    }

    @Test
    fun `empty segments and zero length clips produce nothing`() {
        assertTrue(overlays(emptyList()).isEmpty())
        assertTrue(
            overlays(listOf(TranscriptSegment(0L, 1_000L, "x")), clipDurationMs = 0L).isEmpty()
        )
    }
}
