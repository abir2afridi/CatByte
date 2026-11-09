package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the fixed-centre-playhead geometry.
 *
 * The playhead never moves on screen: the content is padded with half a
 * viewport per side, and the scroll offset for a frame is exactly
 * `frame * pxPerFrame`. These tests guard the bug class where the needle
 * drifted with the playhead instead of staying centred.
 */
class CenterPlayheadTest {

    @Test
    fun `content is padded with half a viewport on each side`() {
        val viewport = 1_000f
        assertEquals(500f, CenterPlayhead.edgePaddingPx(viewport))
        assertEquals(
            2_500f,
            CenterPlayhead.paddedContentWidthPx(timelineWidthPx = 1_500f, viewportWidthPx = viewport)
        )
    }

    @Test
    fun `max offset equals the timeline width`() {
        // content (timeline + viewport) - viewport = timeline width.
        assertEquals(
            1_500f,
            CenterPlayhead.maxOffsetPx(timelineWidthPx = 1_500f, viewportWidthPx = 1_000f)
        )
        // Even a timeline shorter than the viewport scrolls its own width:
        // the two half-viewport pads make the content viewport + timeline.
        assertEquals(
            400f,
            CenterPlayhead.maxOffsetPx(timelineWidthPx = 400f, viewportWidthPx = 1_000f)
        )
    }

    @Test
    fun `time zero sits under the needle without scrolling`() {
        assertEquals(
            0f,
            CenterPlayhead.offsetForFrame(
                playheadFrame = 0L,
                pxPerFrame = 2f,
                timelineWidthPx = 1_500f,
                viewportWidthPx = 1_000f
            )
        )
    }

    @Test
    fun `frame offset is a plain frame times pxPerFrame`() {
        // The leading half-viewport pad already centres time zero, so no
        // extra viewport/2 term may creep back in.
        assertEquals(
            200f,
            CenterPlayhead.offsetForFrame(
                playheadFrame = 100L,
                pxPerFrame = 2f,
                timelineWidthPx = 1_500f,
                viewportWidthPx = 1_000f
            )
        )
    }

    @Test
    fun `offset never scrolls past the end of the timeline`() {
        assertEquals(
            1_500f,
            CenterPlayhead.offsetForFrame(
                playheadFrame = 10_000L,
                pxPerFrame = 2f,
                timelineWidthPx = 1_500f,
                viewportWidthPx = 1_000f
            )
        )
    }

    @Test
    fun `frame under the needle round-trips`() {
        val timelineWidth = 8_000f
        val viewport = 1_000f
        val pxPerFrame = 3.5f
        for (frame in listOf(0L, 1L, 30L, 900L, 2_000L)) {
            val offset = CenterPlayhead.offsetForFrame(
                playheadFrame = frame,
                pxPerFrame = pxPerFrame,
                timelineWidthPx = timelineWidth,
                viewportWidthPx = viewport
            )
            assertEquals(
                "frame $frame",
                frame,
                CenterPlayhead.frameAtPlayhead(offset, pxPerFrame)
            )
        }
    }

    @Test
    fun `frame under the needle ignores negative offsets and bad scales`() {
        assertEquals(0L, CenterPlayhead.frameAtPlayhead(-40f, pxPerFrame = 2f))
        assertEquals(0L, CenterPlayhead.frameAtPlayhead(40f, pxPerFrame = 0f))
        assertEquals(
            0f,
            CenterPlayhead.offsetForFrame(
                playheadFrame = 10L,
                pxPerFrame = 0f,
                timelineWidthPx = 500f,
                viewportWidthPx = 300f
            )
        )
    }

    @Test
    fun `offset is always inside the scrollable range`() {
        val timelineWidth = 3_000f
        val viewport = 1_200f
        val max = CenterPlayhead.maxOffsetPx(timelineWidth, viewport)
        for (frame in 0L..5_000L step 250L) {
            val offset = CenterPlayhead.offsetForFrame(frame, 1.7f, timelineWidth, viewport)
            assertTrue("frame $frame offset $offset", offset in 0f..max)
        }
    }
}
