package com.example.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the fixed-centre-playhead geometry.
 *
 * The playhead never moves on screen: the content is padded with the needle
 * inset on the left and the rest of the viewport on the right, and the scroll
 * offset for a frame is exactly `frame * pxPerFrame`. These tests guard the
 * bug class where the needle drifted with the playhead, and the follow-up bug
 * where it centred on the viewport instead of the whole timeline panel (so it
 * sat to the right of the play button above).
 */
class CenterPlayheadTest {

    @Test
    fun `needle inset centres on the whole panel, not the viewport`() {
        // No gutter: panel == viewport, so the needle is at the viewport centre.
        assertEquals(
            500f,
            CenterPlayhead.needleInsetPx(viewportWidthPx = 1_000f, gutterWidthPx = 0f)
        )
        // 96 px gutter, 224 px viewport: panel = 320, centre = 160, inset = 64.
        assertEquals(
            64f,
            CenterPlayhead.needleInsetPx(viewportWidthPx = 224f, gutterWidthPx = 96f)
        )
        // Gutter wider than the viewport cannot push the needle off the left.
        assertEquals(
            0f,
            CenterPlayhead.needleInsetPx(viewportWidthPx = 50f, gutterWidthPx = 96f)
        )
    }

    @Test
    fun `pads add up to exactly one viewport`() {
        val viewport = 224f
        val inset = CenterPlayhead.needleInsetPx(viewport, gutterWidthPx = 96f)
        val leading = CenterPlayhead.leadingPaddingPx(inset)
        val trailing = CenterPlayhead.trailingPaddingPx(viewport, inset)
        assertEquals(64f, leading)
        assertEquals(160f, trailing)
        assertEquals(viewport, leading + trailing)
    }

    @Test
    fun `padded content is timeline plus one viewport`() {
        // The two pads cancel the viewport, so the content width does not
        // depend on where the needle sits inside it.
        assertEquals(
            2_500f,
            CenterPlayhead.paddedContentWidthPx(timelineWidthPx = 1_500f, viewportWidthPx = 1_000f)
        )
    }

    @Test
    fun `max offset equals the timeline width`() {
        assertEquals(
            1_500f,
            CenterPlayhead.maxOffsetPx(timelineWidthPx = 1_500f, viewportWidthPx = 1_000f)
        )
        // Even a timeline shorter than the viewport scrolls its own width:
        // the pads make the content viewport + timeline.
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
        // The leading pad already centres time zero, so no extra inset term
        // may creep back into the offset.
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
